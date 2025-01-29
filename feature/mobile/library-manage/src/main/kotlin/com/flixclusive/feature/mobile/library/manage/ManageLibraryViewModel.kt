package com.flixclusive.feature.mobile.library.manage

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.util.coroutines.AppDispatchers
import com.flixclusive.core.util.coroutines.asStateFlow
import com.flixclusive.data.library.custom.LibraryListRepository
import com.flixclusive.domain.user.UserSessionManager
import com.flixclusive.feature.mobile.library.manage.LibraryListWithPreview.Companion.toPreview
import com.flixclusive.feature.mobile.library.manage.PreviewPoster.Companion.toPreviewPoster
import com.flixclusive.model.database.LibraryList
import com.flixclusive.model.database.LibraryListWithItems
import com.flixclusive.model.film.Film
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class ManageLibraryViewModel
    @Inject
    constructor(
        private val libraryListRepository: LibraryListRepository,
        userSessionManager: UserSessionManager,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(LibraryUiState())
        val uiState = _uiState.asStateFlow()

        private var addLibJob: Job? = null
        private var removeLibJob: Job? = null
        private var removeSelectionJob: Job? = null

        fun onUpdateFilter(filter: LibrarySortFilter) {
            val isUpdatingDirection = _uiState.value.selectedFilter == filter

            _uiState.update {
                if (isUpdatingDirection) {
                    it.copy(selectedFilterDirection = it.selectedFilterDirection.toggle())
                } else {
                    it.copy(selectedFilter = filter)
                }
            }
        }

        private val currentFilterWithDirection =
            _uiState
                .mapLatest {
                    FilterWithDirection(
                        filter = it.selectedFilter,
                        direction = it.selectedFilterDirection,
                    )
                }.distinctUntilChanged()

        val selectedLibraries =
            _uiState
                .mapLatest { it.selectedLibraries }
                .distinctUntilChanged()
                .asStateFlow(viewModelScope)

        val libraries =
            userSessionManager.currentUser
                .filterNotNull()
                .flatMapLatest { libraryListRepository.getUserWithListsAndItems(it.id) }
                .filterNotNull()
                .mapLatest { userWithLists ->
                    userWithLists.list.map { listWithItems -> listWithItems.toPreview() }
                }.combine(currentFilterWithDirection, ::applyListFilter)
                .asStateFlow(viewModelScope, initialValue = emptyList())

        fun onAddLibrary(list: LibraryList) {
            if (addLibJob?.isActive == true) return

            addLibJob =
                AppDispatchers.IO.scope.launch {
                    libraryListRepository.insertList(list)
                }
        }

        fun onRemoveLongClickedLibrary() {
            if (removeLibJob?.isActive == true) return

            removeLibJob =
                AppDispatchers.IO.scope.launch {
                    val list = _uiState.value.longClickedLibrary?.list ?: return@launch
                    libraryListRepository.deleteListById(list.id)
                }
        }

        fun onRemoveSelection() {
            if (removeSelectionJob?.isActive == true) return

            removeSelectionJob =
                AppDispatchers.IO.scope.launch {
                    selectedLibraries.value.forEach {
                        libraryListRepository.deleteListById(it.list.id)
                    }
                }
        }

        fun onSaveEdits(list: LibraryList) {
            if (addLibJob?.isActive == true) return

            addLibJob =
                AppDispatchers.IO.scope.launch {
                    libraryListRepository.updateList(list)
                    _uiState.update { state ->
                        state.copy(
                            isEditingLibrary = false,
                            longClickedLibrary = null,
                        )
                    }
                }
        }

        fun onStartMultiSelecting() {
            _uiState.update { it.copy(isMultiSelecting = true) }
        }


        fun onToggleSelect(item: LibraryListWithPreview) {
            _uiState.update {
                val newSet = it.selectedLibraries.toMutableSet()
                val isSelected = newSet.contains(item)

                if (isSelected) {
                    newSet.remove(item)
                } else {
                    newSet.add(item)
                }

                it.copy(selectedLibraries = newSet.toSet())
            }
        }

        fun onUnselectAll() {
            _uiState.update {
                it.copy(
                    selectedLibraries = emptySet(),
                    isMultiSelecting = false,
                )
            }
        }

        fun onQueryChange(query: String) {
            _uiState.update { it.copy(searchQuery = query) }
        }

        fun onToggleFilterSheet(isVisible: Boolean) {
            _uiState.update { it.copy(isShowingFilterSheet = isVisible) }
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

        private fun applyListFilter(
            list: List<LibraryListWithPreview>,
            filterWithDirection: FilterWithDirection,
        ): List<LibraryListWithPreview> {
            return list.sortedWith(
                compareBy<LibraryListWithPreview>(
                    selector = {
                        when (filterWithDirection.filter) {
                            LibrarySortFilter.Name -> it.list.name
                            LibrarySortFilter.AddedAt -> it.list.createdAt.time
                            LibrarySortFilter.ModifiedAt -> it.list.updatedAt.time
                            LibrarySortFilter.ItemCount -> it.itemsCount
                        }
                    },
                ).let { comparator ->
                    if (filterWithDirection.direction == LibrarySortFilter.Direction.ASC) {
                        comparator
                    } else {
                        comparator.reversed()
                    }
                },
            )
        }
    }

@Immutable
internal data class LibraryUiState(
    val searchQuery: String = "",
    val isShowingFilterSheet: Boolean = false,
    val isShowingSearchBar: Boolean = false,
    val isMultiSelecting: Boolean = false,
    val isShowingOptionsSheet: Boolean = false,
    val isEditingLibrary: Boolean = false,
    val longClickedLibrary: LibraryListWithPreview? = null,
    val selectedFilter: LibrarySortFilter = LibrarySortFilter.ModifiedAt,
    val selectedFilterDirection: LibrarySortFilter.Direction = LibrarySortFilter.Direction.ASC,
    val selectedLibraries: Set<LibraryListWithPreview> = emptySet(),
)

@Immutable
internal data class LibraryListWithPreview(
    val list: LibraryList,
    val itemsCount: Int,
    val previews: List<PreviewPoster>,
) {
    companion object {
        fun LibraryListWithItems.toPreview(): LibraryListWithPreview {
            return LibraryListWithPreview(
                list = list,
                itemsCount = items.size,
                previews =
                    items
                        .take(3)
                        .map { item -> item.film.toPreviewPoster() },
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
        fun Film.toPreviewPoster(): PreviewPoster {
            return PreviewPoster(
                title = title,
                posterPath = posterImage,
            )
        }
    }
}
