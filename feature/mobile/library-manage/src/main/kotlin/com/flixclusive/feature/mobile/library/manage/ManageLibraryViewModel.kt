package com.flixclusive.feature.mobile.library.manage

import androidx.compose.runtime.Immutable
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.database.entity.film.DBFilm
import com.flixclusive.core.database.entity.library.LibraryList
import com.flixclusive.core.database.entity.library.LibraryListWithItems
import com.flixclusive.data.database.repository.LibraryListRepository
import com.flixclusive.data.database.repository.LibrarySort
import com.flixclusive.data.database.session.UserSessionManager
import com.flixclusive.feature.mobile.library.manage.LibraryListWithPreview.Companion.toPreview
import com.flixclusive.feature.mobile.library.manage.PreviewPoster.Companion.toPreviewPoster
import com.flixclusive.model.film.Film
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
internal class ManageLibraryViewModel @Inject constructor(
    private val libraryListRepository: LibraryListRepository,
    private val userSessionManager: UserSessionManager,
    private val appDispatchers: AppDispatchers,
) : ViewModel() {
    private var addLibJob: Job? = null
    private var removeLibJob: Job? = null
    private var removeSelectionJob: Job? = null

    private val _uiState = MutableStateFlow(ManageLibraryUiState())
    val uiState = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedLibraries = MutableStateFlow(persistentSetOf<LibraryListWithPreview>())
    val selectedLibraries = _selectedLibraries.asStateFlow()

    val libraries = combine(
        flow = userSessionManager.currentUser.filterNotNull(),
        flow2 = uiState.map { it.selectedFilter }.distinctUntilChanged(),
    ) { user, filter ->
        user.id to filter
    }.flatMapLatest { (user, filter) ->
        libraryListRepository
            .getListsAndItems(userId = user, sort = filter)
            .mapLatest { data ->
                data.fastMap {
                    it.toPreview()
                }.toPersistentList()
            }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = persistentListOf(),
    )

    val searchResults = searchQuery
        .debounce(800) // Debounce to avoid excessive computations while typing
        .distinctUntilChanged()
        .filter { it.isNotEmpty() }
        .flatMapLatest { query ->
            libraries.mapLatest { list ->
                list.fastFilter { library ->
                    if (library.name.contains(query, true)) {
                        return@fastFilter true
                    }

                    library.description?.let { description ->
                        if (description.contains(query, true)) {
                            return@fastFilter true
                        }
                    }

                    false
                }.toPersistentList()
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = persistentListOf(),
        )

    fun onUpdateFilter(filter: LibrarySort) {
        val isUpdatingDirection = _uiState.value.selectedFilter == filter

        _uiState.update {
            if (isUpdatingDirection) {
                it.copy(selectedFilter = it.selectedFilter.toggleAscending())
            } else {
                it.copy(selectedFilter = filter)
            }
        }
    }

    fun onRemoveLongClickedLibrary() {
        if (removeLibJob?.isActive == true) return

        val listId = _uiState.value.longClickedLibrary?.id
        requireNotNull(listId) { "No library selected for removal!" }

        removeLibJob = appDispatchers.ioScope.launch {
            libraryListRepository.deleteListById(listId)
        }
    }

    fun onRemoveSelection() {
        if (removeSelectionJob?.isActive == true) return

        removeSelectionJob = appDispatchers.ioScope.launch {
            selectedLibraries.value.forEach {
                libraryListRepository.deleteListById(it.id)
            }
        }
    }

    fun onSaveEdits(list: LibraryList) {
        if (addLibJob?.isActive == true) return

        addLibJob = appDispatchers.ioScope.launch {
            libraryListRepository.updateList(list)
            _uiState.update { state ->
                state.copy(
                    isEditingLibrary = false,
                    longClickedLibrary = null,
                )
            }
        }
    }

    fun onAdd(
        name: String,
        description: String?,
    ) {
        if (addLibJob?.isActive == true) return

        addLibJob = appDispatchers.ioScope.launch {
            val userId = userSessionManager.currentUser.value?.id ?: return@launch
            val list =
                LibraryList(
                    id = 0,
                    ownerId = userId,
                    name = name,
                    description = description,
                )

            libraryListRepository.insertList(list)
            _uiState.update { state ->
                state.copy(isCreatingLibrary = false)
            }
        }
    }

    fun onStartMultiSelecting() {
        _uiState.update { it.copy(isMultiSelecting = true) }
    }

    fun onToggleSelect(item: LibraryListWithPreview) {
        _selectedLibraries.update {
            val isSelected = it.contains(item)

            if (isSelected) {
                it.remove(item)
            } else {
                it.add(item)
            }
        }
    }

    fun onUnselectAll() {
        _selectedLibraries.value = persistentSetOf()
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

    fun onToggleOptionsSheet(isVisible: Boolean) {
        _uiState.update { it.copy(isShowingOptionsSheet = isVisible) }
    }

    fun onLongClickItem(library: LibraryListWithPreview?) {
        _uiState.update { it.copy(longClickedLibrary = library) }
    }

    fun onToggleEditDialog(isVisible: Boolean) {
        _uiState.update {
            if (isVisible) {
                it.copy(
                    isShowingOptionsSheet = false,
                    isEditingLibrary = true,
                )
            } else {
                it.copy(
                    isEditingLibrary = false,
                    longClickedLibrary = null,
                )
            }
        }
    }

    fun onToggleCreateDialog(isVisible: Boolean) {
        _uiState.update {
            it.copy(isCreatingLibrary = isVisible)
        }
    }
}

@Immutable
internal data class ManageLibraryUiState(
    val isShowingFilterSheet: Boolean = false,
    val isShowingSearchBar: Boolean = false,
    val isMultiSelecting: Boolean = false,
    val isShowingOptionsSheet: Boolean = false,
    val isCreatingLibrary: Boolean = false,
    val isEditingLibrary: Boolean = false,
    val longClickedLibrary: LibraryListWithPreview? = null,
    val selectedFilter: LibrarySort = LibrarySort.Modified(ascending = false),
)

@Immutable
internal data class LibraryListWithPreview(
    val list: LibraryList,
    val itemsCount: Int,
    val previews: List<PreviewPoster>,
) {
    val name get() = list.name
    val description get() = list.description
    val id get() = list.id

    companion object {
        fun LibraryListWithItems.toPreview(): LibraryListWithPreview {
            return LibraryListWithPreview(
                list = list,
                itemsCount = items.size,
                previews = items
                    .takeLast(3)
                    .sortedByDescending { it.item.updatedAt }
                    .map { item -> item.metadata.toPreviewPoster() },
            )
        }
    }
}

@Immutable
internal data class PreviewPoster(
    val title: String?,
    val posterPath: String?,
) {
    companion object {
        fun DBFilm.toPreviewPoster(): PreviewPoster {
            return PreviewPoster(
                title = title,
                posterPath = posterImage,
            )
        }

        fun Film.toPreviewPoster(): PreviewPoster {
            return PreviewPoster(
                title = title,
                posterPath = posterImage,
            )
        }
    }
}
