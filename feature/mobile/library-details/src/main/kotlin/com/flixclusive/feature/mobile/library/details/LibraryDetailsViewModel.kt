package com.flixclusive.feature.mobile.library.details

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.common.domain.Async
import com.flixclusive.core.common.domain.PagingState
import com.flixclusive.core.common.locale.UiText
import com.flixclusive.core.database.entity.library.LibraryList
import com.flixclusive.core.database.entity.library.LibraryListItem
import com.flixclusive.core.database.entity.library.LibraryListItemWithMetadata
import com.flixclusive.core.database.entity.media.DBMedia.Companion.toDBMedia
import com.flixclusive.core.database.entity.media.DBMediaExternalId.Companion.toDBMediaExternalIds
import com.flixclusive.core.util.log.errorLog
import com.flixclusive.data.database.repository.LibraryListRepository
import com.flixclusive.data.database.repository.LibrarySort
import com.flixclusive.domain.provider.usecase.tracker.GetTrackerApiUseCase
import com.flixclusive.domain.provider.usecase.tracker.GetTrackerListItemsUseCase
import com.flixclusive.model.media.MediaMetadata
import com.flixclusive.provider.tracker.TrackerList
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date
import kotlin.time.Duration.Companion.milliseconds

private const val PAGINATE_SIZE = 20

@OptIn(FlowPreview::class)
@HiltViewModel(assistedFactory = LibraryDetailsViewModel.Factory::class)
class LibraryDetailsViewModel @AssistedInject constructor(
    private val libraryListRepository: LibraryListRepository,
    private val appDispatchers: AppDispatchers,
    private val getTrackerListItems: GetTrackerListItemsUseCase,
    private val getTrackerApi: GetTrackerApiUseCase,
    @Assisted private val navArgs: LibraryDetailsNavArgs,
) : ViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(navArgs: LibraryDetailsNavArgs): LibraryDetailsViewModel
    }

    private var removeSelectionJob: Job? = null
    private var paginateJob: Job? = null

    private val _uiState = MutableStateFlow(LibraryDetailsUiState())
    val uiState = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _trackerError = MutableSharedFlow<UiText>()
    val trackerError = _trackerError.asSharedFlow()

    val selectedItems = mutableStateSetOf<LibraryListItemWithMetadata>()

    val items = mutableStateSetOf<LibraryListItemWithMetadata>()

    private val _library = MutableStateFlow<LibraryList>(navArgs.library)
    val library = _library.asStateFlow()

    val searchItems = searchQuery
        .debounce(800.milliseconds) // Debounce to avoid excessive computations while typing
        .distinctUntilChanged()
        .filter { it.isNotEmpty() }
        .flatMapLatest { query ->
            if (navArgs.tracker != null) {
                val list = items.filter {
                    it.metadata.title.contains(query, ignoreCase = true) &&
                        it.metadata.overview?.contains(query, ignoreCase = true) == true
                }

                flowOf(list.toSet())
            } else {
                libraryListRepository
                    .searchItems(
                        query = query,
                        listId = navArgs.library.id,
                        sort = uiState.value.selectedFilter,
                    ).mapLatest { it.toSet() }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptySet(),
        )

    init {
        paginate()

        viewModelScope.launch {
            if (navArgs.tracker != null) return@launch

            libraryListRepository
                .getList(navArgs.library.id)
                .filterNotNull()
                .collect {
                    _library.value = it
                }
        }
    }

    private suspend fun paginateAppList() {
        val results = libraryListRepository.paginateItems(
            listId = navArgs.library.id,
            page = uiState.value.currentPage,
            sort = uiState.value.selectedFilter,
            pageSize = PAGINATE_SIZE
        )

        if (results.size < PAGINATE_SIZE) {
            _uiState.update { it.copy(pagingState = PagingState.Exhausted) }
        } else {
            _uiState.update {
                it.copy(
                    pagingState = PagingState.Idle,
                    currentPage = it.currentPage + 1,
                )
            }
        }

        items.addAll(results)
    }

    private suspend fun paginateTrackerList() {
        val trackerList = navArgs.library.toTrackerList(
            providerId = navArgs.tracker!!.id,
        )

        getTrackerListItems(
            list = trackerList,
            page = uiState.value.currentPage,
        ).collect { state ->
            when (state) {
                is Async.Loading -> {
                    _uiState.update {
                        it.copy(pagingState = PagingState.Loading)
                    }
                }

                is Async.Failure -> {
                    _uiState.update {
                        it.copy(pagingState = PagingState.Error(error = state.message))
                    }
                }

                is Async.Success -> {
                    val list = state.data.results.fastMap {
                        it.toLibraryListItemWithMetadata(navArgs.library.id)
                    }

                    items.addAll(list)
                    _uiState.update {
                        it.copy(
                            pagingState = if (state.data.hasNextPage) PagingState.Idle else PagingState.Exhausted,
                            currentPage = it.currentPage + 1,
                        )
                    }
                }
            }
        }
    }

    private suspend fun removeItem(item: LibraryListItemWithMetadata) {
        if (navArgs.tracker != null) {
            try {
                val api = getTrackerApi(navArgs.tracker.id)
                api.removeListItem(
                    list = navArgs.library.toTrackerList(navArgs.tracker.id),
                    item = item.toMediaMetadata(),
                )

                items.remove(item)
                _library.value = _library.value.copy(
                    updatedAt = Date(),
                )
            } catch (e: Throwable) {
                errorLog("Failed to remove item from tracker list: ${e.message}")
                e.printStackTrace()

                _trackerError.emit(UiText.from(e.message ?: "Unknown error"))
            }
        } else {
            libraryListRepository
                .deleteItem(itemId = item.itemId)

            items.remove(item)
        }
    }

    fun paginate() {
        if (paginateJob?.isActive == true) return
        if (uiState.value.pagingState.isExhausted) return

        paginateJob = viewModelScope.launch {
            _uiState.update { it.copy(pagingState = PagingState.Loading) }
            if (navArgs.tracker != null) {
                paginateTrackerList()
            } else {
                paginateAppList()
            }
        }
    }

    fun onUpdateFilter(filter: LibrarySort) {
        _uiState.update {
            if (filter == it.selectedFilter) {
                it.copy(selectedFilter = it.selectedFilter.toggleAscending())
            } else {
                it.copy(selectedFilter = filter)
            }
        }
    }

    fun onRemoveSelection() {
        if (removeSelectionJob?.isActive == true) return

        removeSelectionJob = appDispatchers.ioScope.launch {
            selectedItems.forEach {
                removeItem(it)
            }
        }
    }

    fun onStartMultiSelecting() {
        _uiState.update { it.copy(isMultiSelecting = true) }
    }

    fun onToggleSelect(item: LibraryListItemWithMetadata) {
        val isSelected = selectedItems.contains(item)

        if (isSelected) {
            selectedItems.remove(item)
        } else {
            selectedItems.add(item)
        }
    }

    fun onUnselectAll() {
        selectedItems.clear()
        _uiState.update {
            it.copy(isMultiSelecting = false)
        }
    }

    fun onQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onToggleSearchBar(isVisible: Boolean) {
        _uiState.update { it.copy(isShowingSearchBar = isVisible) }
    }
}

private fun MediaMetadata.toLibraryListItemWithMetadata(
    listId: String,
): LibraryListItemWithMetadata {
    return LibraryListItemWithMetadata(
        item = LibraryListItem(
            mediaId = id,
            id = id,
            listId = listId,
        ),
        metadata = toDBMedia(),
        externalIds = toDBMediaExternalIds(),
    )
}

fun LibraryList.toTrackerList(
    providerId: String,
): TrackerList {
    return TrackerList(
        id = id,
        providerId = providerId,
        name = name,
        description = description,
        createdAt = createdAt.time,
        updatedAt = updatedAt.time,
        images = emptyList(),
    )
}

@Immutable
data class LibraryDetailsUiState(
    val isShowingSearchBar: Boolean = false,
    val isMultiSelecting: Boolean = false,
    val selectedFilter: LibrarySort = LibrarySort.Added(ascending = false),
    val currentPage: Int = 1,
    val pagingState: PagingState = PagingState.Idle,
)
