package com.flixclusive.feature.mobile.library.details

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.database.entity.library.LibraryListItemWithMetadata
import com.flixclusive.data.database.repository.LibraryListRepository
import com.flixclusive.data.database.repository.LibrarySort
import com.ramcosta.composedestinations.generated.librarydetails.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
internal class LibraryDetailsViewModel @Inject constructor(
    private val libraryListRepository: LibraryListRepository,
    private val appDispatchers: AppDispatchers,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val navArgs = savedStateHandle.navArgs<LibraryDetailsNavArgs>()

    private var removeJob: Job? = null
    private var removeSelectionJob: Job? = null

    private val _uiState = MutableStateFlow(LibraryDetailsUiState())
    val uiState = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedItems = MutableStateFlow(persistentSetOf<LibraryListItemWithMetadata>())
    val selectedItems = _selectedItems.asStateFlow()

    val library = libraryListRepository
        .getList(navArgs.library.id)
        .filterNotNull()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = navArgs.library,
        )

    val items = _uiState
        .map { it.selectedFilter }
        .distinctUntilChanged()
        .flatMapLatest { filter ->
            libraryListRepository.getItems(
                listId = navArgs.library.id,
                sort = filter,
            ).map { list -> list.toPersistentList() }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = persistentListOf(),
        )

    val searchItems = searchQuery
        .debounce(800) // Debounce to avoid excessive computations while typing
        .distinctUntilChanged()
        .filter { it.isNotEmpty() }
        .flatMapLatest { query ->
            libraryListRepository.searchItems(
                query = query,
                listId = navArgs.library.id,
                sort = uiState.value.selectedFilter,
            ).map { list -> list.toPersistentList() }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = persistentListOf(),
        )

    fun onUpdateFilter(filter: LibrarySort) {
        _uiState.update {
            if (filter == it.selectedFilter) {
                it.copy(selectedFilter = it.selectedFilter.toggleAscending())
            } else {
                it.copy(selectedFilter = filter)
            }
        }
    }

    fun onRemoveLongClickedItem() {
        if (removeJob?.isActive == true) return

        val item = _uiState.value.longClickedItem
        requireNotNull(item) {
            "Long clicked item should not be null when trying to remove it."
        }

        removeJob = appDispatchers.ioScope.launch {
            libraryListRepository
                .deleteItem(itemId = item.itemId)
        }
    }

    fun onRemoveSelection() {
        if (removeSelectionJob?.isActive == true) return

        removeSelectionJob = appDispatchers.ioScope.launch {
            selectedItems.value.forEach {
                libraryListRepository.deleteItem(itemId = it.item.id)
            }
        }
    }

    fun onStartMultiSelecting() {
        _uiState.update { it.copy(isMultiSelecting = true) }
    }

    fun onToggleSelect(item: LibraryListItemWithMetadata) {
        _selectedItems.update {
            val isSelected = it.contains(item)

            if (isSelected) {
                it.remove(item)
            } else {
                it.add(item)
            }
        }
    }

    fun onUnselectAll() {
        _selectedItems.update { persistentSetOf() }
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

    fun onLongClickItem(item: LibraryListItemWithMetadata?) {
        _uiState.update { it.copy(longClickedItem = item) }
    }
}

@Immutable
internal data class LibraryDetailsUiState(
    val isLoading: Boolean = false,
    val isShowingSearchBar: Boolean = false,
    val isMultiSelecting: Boolean = false,
    val longClickedItem: LibraryListItemWithMetadata? = null,
    val selectedFilter: LibrarySort = LibrarySort.Added(ascending = false),
)
