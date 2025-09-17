package com.flixclusive.feature.mobile.library.details

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.compose.ui.util.fastFilter
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.common.collections.SortUtils
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.common.locale.UiText
import com.flixclusive.core.database.entity.library.LibraryListItemWithMetadata
import com.flixclusive.core.database.entity.library.LibraryListWithItems
import com.flixclusive.data.database.repository.LibraryListRepository
import com.flixclusive.data.database.repository.WatchProgressRepository
import com.flixclusive.data.database.repository.WatchlistRepository
import com.flixclusive.feature.mobile.library.common.util.LibraryListUtil
import com.flixclusive.feature.mobile.library.common.util.LibraryMapper.toWatchProgressLibraryList
import com.flixclusive.feature.mobile.library.common.util.LibraryMapper.toWatchlistLibraryList
import com.flixclusive.feature.mobile.library.common.util.LibrarySortFilter
import com.flixclusive.feature.mobile.library.details.util.FilmUtils.matches
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
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
import com.flixclusive.core.strings.R as LocaleR

@OptIn(FlowPreview::class)
@HiltViewModel
internal class LibraryDetailsViewModel
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val libraryListRepository: LibraryListRepository,
        private val watchProgressRepository: WatchProgressRepository,
        private val watchlistRepository: WatchlistRepository,
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

        val library = getLibrary()
            .mapLatest { it.list }
            .distinctUntilChanged()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = navArgs.library,
            )

        val items = combine(
            _uiState.map { it.selectedFilter }.distinctUntilChanged(),
            _uiState.map { it.isSortingAscending }.distinctUntilChanged(),
            getLibrary(),
        ) { filter, ascending, items ->
            items.sort(
                filter = filter,
                ascending = ascending,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = persistentListOf(),
        )

        val searchItems = searchQuery
            .debounce(800) // Debounce to avoid excessive computations while typing
            .distinctUntilChanged()
            .filter { it.isNotEmpty() }
            .flatMapLatest { query ->
                items.mapLatest { list ->
                    list
                        .fastFilter { item -> item.metadata.matches(query) }
                        .toPersistentList()
                }
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = persistentListOf(),
            )

        fun onUpdateFilter(filter: LibrarySortFilter) {
            _uiState.update {
                if (filter == it.selectedFilter) {
                    it.copy(isSortingAscending = !it.isSortingAscending)
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
                    libraryListRepository.deleteItem(itemId = it.itemId)
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

        private fun getLibrary(): Flow<LibraryListWithItems> {
            return when (navArgs.library.id) {
                LibraryListUtil.WATCHLIST_LIB_ID -> getWatchlistItemsFlow()
                LibraryListUtil.WATCH_PROGRESS_LIB_ID -> getWatchHistoryItemsFlow()
                else -> getCustomLibraryItemsFlow()
            }
        }

        private fun getCustomLibraryItemsFlow(): Flow<LibraryListWithItems> {
            return libraryListRepository
                .getListWithItems(listId = navArgs.library.id)
                .filterNotNull()
        }

        private fun getWatchlistItemsFlow(): Flow<LibraryListWithItems> {
            return watchlistRepository
                .getAllAsFlow(navArgs.library.ownerId)
                .filterNotNull()
                .mapLatest { items ->
                    items.toWatchlistLibraryList(context)
                }
        }

        private fun getWatchHistoryItemsFlow(): Flow<LibraryListWithItems> {
            return watchProgressRepository
                .getAllAsFlow(navArgs.library.ownerId)
                .filterNotNull()
                .mapLatest { items ->
                    items.toWatchProgressLibraryList(context)
                }
        }

        private fun LibraryListWithItems.sort(
            filter: LibrarySortFilter,
            ascending: Boolean,
        ) = this@sort.items
            .sortedWith(
                SortUtils.compareBy<LibraryListItemWithMetadata>(
                    ascending = ascending,
                    selector = {
                        when (filter) {
                            LibrarySortFilter.Name -> it.metadata.title
                            LibrarySortFilter.AddedAt -> it.item.addedAt.time
                            LibraryDetailsFilters.Rating -> it.metadata.rating
                            LibraryDetailsFilters.Year -> it.metadata.year
                            else -> throw Error()
                        }
                    },
                ),
            ).toPersistentList()
    }

@Immutable
internal data class LibraryDetailsUiState(
    val isLoading: Boolean = false,
    val isShowingSearchBar: Boolean = false,
    val isMultiSelecting: Boolean = false,
    val longClickedItem: LibraryListItemWithMetadata? = null,
    val selectedFilter: LibrarySortFilter = LibrarySortFilter.AddedAt,
    val isSortingAscending: Boolean = true,
)

internal object LibraryDetailsFilters {
    @Immutable
    data object Year : LibrarySortFilter {
        override val displayName = UiText.from(LocaleR.string.year)
    }

    @Immutable
    data object Rating : LibrarySortFilter {
        override val displayName = UiText.from(LocaleR.string.rating)
    }

    val defaultFilters =
        persistentListOf(
            LibrarySortFilter.AddedAt,
            LibrarySortFilter.Name,
            Year,
            Rating,
        )
}
