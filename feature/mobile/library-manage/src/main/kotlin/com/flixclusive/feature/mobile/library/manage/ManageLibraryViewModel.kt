package com.flixclusive.feature.mobile.library.manage

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.database.entity.LibraryList
import com.flixclusive.core.database.entity.LibraryListWithItems
import com.flixclusive.core.strings.UiText
import com.flixclusive.core.util.coroutines.AppDispatchers
import com.flixclusive.core.util.coroutines.asStateFlow
import com.flixclusive.data.library.custom.LibraryListRepository
import com.flixclusive.data.library.recent.WatchHistoryRepository
import com.flixclusive.data.library.watchlist.WatchlistRepository
import com.flixclusive.domain.session.UserSessionManager
import com.flixclusive.feature.mobile.library.common.util.FilterWithDirection
import com.flixclusive.feature.mobile.library.common.util.LibraryFilterDirection
import com.flixclusive.feature.mobile.library.common.util.LibraryListUtil
import com.flixclusive.feature.mobile.library.common.util.LibrarySortFilter
import com.flixclusive.feature.mobile.library.manage.LibraryListWithPreview.Companion.toPreview
import com.flixclusive.feature.mobile.library.manage.PreviewPoster.Companion.toPreviewPoster
import com.flixclusive.feature.mobile.library.manage.util.filter
import com.flixclusive.feature.mobile.library.manage.util.toUiLibraryList
import com.flixclusive.model.film.Film
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.persistentListOf
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
import com.flixclusive.core.strings.R as LocaleR

@HiltViewModel
internal class ManageLibraryViewModel
    @Inject
    constructor(
        private val libraryListRepository: LibraryListRepository,
        private val watchHistoryRepository: WatchHistoryRepository,
        private val watchlistRepository: WatchlistRepository,
        private val userSessionManager: UserSessionManager,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(ManageLibraryUiState())
        val uiState = _uiState.asStateFlow()

        private var addLibJob: Job? = null
        private var removeLibJob: Job? = null
        private var removeSelectionJob: Job? = null

        private val currentFilterWithDirection =
            _uiState
                .mapLatest {
                    FilterWithDirection(
                        filter = it.selectedFilter,
                        direction = it.selectedFilterDirection,
                        searchQuery = it.searchQuery,
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
                .flatMapLatest {
                    combine(
                        libraryListRepository.getUserWithListsAndItems(it.id),
                        watchHistoryRepository.getAllItemsInFlow(it.id),
                        watchlistRepository.getAllItemsInFlow(it.id),
                    ) { userWithLists, watchHistory, watchlist ->
                        Triple(userWithLists, watchHistory, watchlist)
                    }
                }.mapLatest { (userWithLists, watchHistory, watchlist) ->
                    userWithLists.list.map { listWithItems -> listWithItems.toPreview() } +
                        watchHistory.toUiLibraryList(
                            id = LibraryListUtil.WATCH_HISTORY_LIB_ID,
                            userId = userWithLists.user.id,
                            searchableName = LibraryListUtil.WATCH_HISTORY_SEARCHABLE_NAME,
                            name = UiText.from(LocaleR.string.recently_watched),
                            description = UiText.from(LocaleR.string.recently_watched_description),
                        ) +
                        watchlist.toUiLibraryList(
                            id = LibraryListUtil.WATCHLIST_LIB_ID,
                            userId = userWithLists.user.id,
                            searchableName = LibraryListUtil.WATCHLIST_SEARCHABLE_NAME,
                            name = UiText.from(LocaleR.string.watchlist),
                            description = UiText.from(LocaleR.string.watchlist_description),
                        )
                }.combine(
                    currentFilterWithDirection,
                ) { list, filterWithDirection ->
                    list.filter(filterWithDirection)
                }.asStateFlow(viewModelScope, initialValue = emptyList())

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

        fun onAdd(
            name: String,
            description: String?,
        ) {
            if (addLibJob?.isActive == true) return

            addLibJob =
                AppDispatchers.IO.scope.launch {
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
    val searchQuery: String = "",
    val isShowingFilterSheet: Boolean = false,
    val isShowingSearchBar: Boolean = false,
    val isMultiSelecting: Boolean = false,
    val isShowingOptionsSheet: Boolean = false,
    val isCreatingLibrary: Boolean = false,
    val isEditingLibrary: Boolean = false,
    val longClickedLibrary: LibraryListWithPreview? = null,
    val selectedFilter: LibrarySortFilter = LibrarySortFilter.ModifiedAt,
    val selectedFilterDirection: LibraryFilterDirection = LibraryFilterDirection.ASC,
    val selectedLibraries: Set<LibraryListWithPreview> = emptySet(),
)

internal sealed interface UiLibraryList

@Immutable
internal data class LibraryListWithPreview(
    val list: LibraryList,
    val itemsCount: Int,
    val previews: List<PreviewPoster>,
) : UiLibraryList {
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

@Immutable
internal data class EmphasisLibraryList(
    val library: LibraryListWithPreview,
    val name: UiText,
    val description: UiText,
) : UiLibraryList

internal val defaultManageLibraryFilters = persistentListOf(
    LibrarySortFilter.Name,
    LibrarySortFilter.AddedAt,
    LibrarySortFilter.ModifiedAt,
    ItemCount,
)
