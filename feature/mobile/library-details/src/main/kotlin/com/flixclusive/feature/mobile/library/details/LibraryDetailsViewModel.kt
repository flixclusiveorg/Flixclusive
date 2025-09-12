package com.flixclusive.feature.mobile.library.details

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.database.entity.LibraryList
import com.flixclusive.core.strings.UiText
import com.flixclusive.core.util.coroutines.AppDispatchers
import com.flixclusive.core.util.coroutines.asStateFlow
import com.flixclusive.data.library.custom.LibraryListRepository
import com.flixclusive.data.library.recent.WatchHistoryRepository
import com.flixclusive.data.library.watchlist.WatchlistRepository
import com.flixclusive.feature.mobile.library.common.util.FilterWithDirection
import com.flixclusive.feature.mobile.library.common.util.LibraryFilterDirection
import com.flixclusive.feature.mobile.library.common.util.LibraryListUtil
import com.flixclusive.feature.mobile.library.common.util.LibrarySortFilter
import com.flixclusive.model.film.Film
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import com.flixclusive.core.strings.R as LocaleR

@HiltViewModel
internal class LibraryDetailsViewModel
    @Inject
    constructor(
        private val libraryListRepository: LibraryListRepository,
        private val watchHistoryRepository: WatchHistoryRepository,
        private val watchlistRepository: WatchlistRepository,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val libraryArgs = savedStateHandle.navArgs<LibraryDetailsNavArgs>().library

        private var removeJob: Job? = null
        private var removeSelectionJob: Job? = null

        val library = getLibraryAsFlow().asStateFlow(viewModelScope)

        private val _uiState = MutableStateFlow(LibraryDetailsUiState.createFrom(libraryArgs.id))
        val uiState = _uiState.asStateFlow()

        private val currentFilterWithDirection = _uiState
            .mapLatest {
                FilterWithDirection(
                    filter = it.selectedFilter,
                    direction = it.selectedFilterDirection,
                    searchQuery = it.searchQuery,
                )
            }.distinctUntilChanged()

        val items = combine(
            currentFilterWithDirection,
            getItems(),
        ) { (filter, direction, query), items ->
            // First, filter items based on search query
            val filteredItems = if (query.isBlank()) {
                items
            } else {
                items.filter { item ->
                    searchInFilm(item.film, query)
                }
            }

            // Then, sort the filtered items
            when (filter) {
                LibrarySortFilter.AddedAt -> filteredItems.sortedWith(
                    compareBy(
                        filterDirection = direction,
                        selector = { it.addedAt },
                    ),
                )

                LibrarySortFilter.Name -> filteredItems.sortedWith(
                    compareBy(
                        filterDirection = direction,
                        selector = { it.film.title },
                    ),
                )

                LibraryDetailsFilters.Year -> filteredItems.sortedWith(
                    compareBy(
                        filterDirection = direction,
                        selector = { it.film.year },
                    ),
                )

                LibraryDetailsFilters.Rating -> filteredItems.sortedWith(
                    compareBy(
                        filterDirection = direction,
                        selector = { it.film.rating },
                    ),
                )

                else -> throw IllegalStateException("Unknown filter: $filter")
            }
        }.asStateFlow(
            scope = viewModelScope,
            initialValue = emptyList(),
        )

        val selectedItems =
            _uiState
                .mapLatest { it.selectedItems }
                .distinctUntilChanged()
                .asStateFlow(viewModelScope)

        fun onUpdateFilter(filter: LibrarySortFilter) {
            _uiState.update {
                if (filter == it.selectedFilter) {
                    it.copy(selectedFilterDirection = it.selectedFilterDirection.toggle())
                } else {
                    it.copy(selectedFilter = filter)
                }
            }
        }

        fun onRemoveLongClickedItem() {
            if (removeJob?.isActive == true) return

            removeJob =
                AppDispatchers.IO.scope.launch {
                    val item = _uiState.value.longClickedFilm ?: return@launch
                    libraryListRepository.deleteItemFromList(
                        listId = libraryArgs.id,
                        itemId = item.identifier,
                    )
                }
        }

        fun onRemoveSelection() {
            if (removeSelectionJob?.isActive == true) return

            removeSelectionJob =
                AppDispatchers.IO.scope.launch {
                    selectedItems.value.forEach {
                        libraryListRepository.deleteItemFromList(
                            listId = libraryArgs.id,
                            itemId = it.identifier,
                        )
                    }
                }
        }

        fun onStartMultiSelecting() {
            _uiState.update { it.copy(isMultiSelecting = true) }
        }

        fun onToggleSelect(item: Film) {
            _uiState.update {
                val newSet = it.selectedItems.toMutableSet()
                val isSelected = newSet.contains(item)

                if (isSelected) {
                    newSet.remove(item)
                } else {
                    newSet.add(item)
                }

                it.copy(selectedItems = newSet.toSet())
            }
        }

        fun onUnselectAll() {
            _uiState.update {
                it.copy(
                    selectedItems = emptySet(),
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

        fun onLongClickItem(film: Film?) {
            _uiState.update { it.copy(longClickedFilm = film) }
        }

        private fun getLibraryAsFlow(): Flow<LibraryList> {
            val type = LibraryType.from(libraryArgs.id)
            return when (type) {
                LibraryType.Custom -> libraryListRepository.getList(listId = libraryArgs.id)
                else -> flowOf(libraryArgs)
            }.filterNotNull()
        }

        private fun getItems(): Flow<List<FilmWithAddedTime>> {
            val type = LibraryType.from(libraryArgs.id)
            return when (type) {
                LibraryType.Custom -> getCustomLibraryItems()
                LibraryType.Watchlist -> getWatchlistItems()
                LibraryType.WatchHistory -> getWatchHistoryItems()
            }
        }

        private fun getCustomLibraryItems(): Flow<List<FilmWithAddedTime>> {
            return libraryListRepository
                .getListWithItems(listId = libraryArgs.id)
                .filterNotNull()
                .mapLatest { (list, items) ->
                    items.fastMap { item ->
                        val crossRef =
                            libraryListRepository
                                .getCrossRef(
                                    listId = list.id,
                                    itemId = item.id,
                                ).filterNotNull()
                                .first()

                        FilmWithAddedTime.from(
                            film = item.film,
                            addedAt = crossRef.addedAt,
                        )
                    }
                }
        }

        private fun getWatchlistItems(): Flow<List<FilmWithAddedTime>> {
            return watchlistRepository
                .getAllItemsInFlow(libraryArgs.ownerId)
                .filterNotNull()
                .mapLatest { items ->
                    items.fastMap { item ->
                        FilmWithAddedTime.from(
                            film = item.film,
                            addedAt = item.addedOn,
                        )
                    }
                }
        }

        private fun getWatchHistoryItems(): Flow<List<FilmWithAddedTime>> {
            return watchHistoryRepository
                .getAllItemsInFlow(libraryArgs.ownerId)
                .filterNotNull()
                .mapLatest { items ->
                    items.fastMap { item ->
                        FilmWithAddedTime.from(
                            film = item.film,
                            addedAt = item.dateWatched,
                        )
                    }
                }
        }

        private fun searchInFilm(film: Film, query: String): Boolean {
            if (film.title.contains(query, true)) {
                return true
            }

            film.overview?.let { overview ->
                if (overview.contains(query, true)) {
                    return true
                }
            }

            film.genres.forEach { genre ->
                if (genre.name.contains(query, true)) {
                    return true
                }
            }

            film.language?.let { language ->
                if (language.contains(query, true)) {
                    return true
                }

                if (Locale(language).displayLanguage.contains(query, true)) {
                    return true
                }
            }

            if (film.providerId.contains(query, true)) {
                return true
            }

            film.year?.let { year ->
                if (year.toString().contains(query, true)) {
                    return true
                }
            }

            return false
        }
    }

@Stable
internal enum class LibraryType {
    Custom,
    Watchlist,
    WatchHistory,
    ;

    companion object {
        fun from(id: Int): LibraryType {
            return when (id) {
                LibraryListUtil.WATCHLIST_LIB_ID -> Watchlist
                LibraryListUtil.WATCH_PROGRESS_LIB_ID -> WatchHistory
                else -> Custom
            }
        }
    }
}

@Immutable
internal data class LibraryDetailsUiState(
    val searchQuery: String = "",
    val errorApiMessage: String? = null,
    val isLoading: Boolean = false,
    val isShowingSearchBar: Boolean = false,
    val isMultiSelecting: Boolean = false,
    val longClickedFilm: Film? = null,
    val libraryType: LibraryType = LibraryType.Custom,
    val selectedFilter: LibrarySortFilter = LibrarySortFilter.AddedAt,
    val selectedFilterDirection: LibraryFilterDirection = LibraryFilterDirection.ASC,
    val selectedItems: Set<Film> = emptySet(),
) {
    companion object {
        fun createFrom(id: Int): LibraryDetailsUiState {
            val type = LibraryType.from(id)
            return LibraryDetailsUiState(libraryType = type)
        }
    }
}

@Immutable
internal data class FilmWithAddedTime(
    val film: Film,
    val addedAt: Date,
) {
    companion object {
        fun from(
            film: Film,
            addedAt: Date,
        ): FilmWithAddedTime {
            return FilmWithAddedTime(
                film = film,
                addedAt = addedAt,
            )
        }
    }

    /**
     * Extracts a unique key for the film based on its identifier and provider ID.
     * If both are blank, it falls back to using the film's title and the time of addition.
     * This key is used to uniquely identify the film in the library.
     * */
    val key: String
        get() {
            var key = film.identifier + film.providerId

            if (key.isBlank()) {
                key = film.title + addedAt.time
            }

            return key
        }
}

internal object LibraryDetailsFilters {
    data object Year : LibrarySortFilter {
        override val displayName = UiText.from(LocaleR.string.year)
    }

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

private inline fun <T> compareBy(
    filterDirection: LibraryFilterDirection,
    crossinline selector: (T) -> Comparable<*>?,
): Comparator<T> =
    Comparator { a, b ->
        if (filterDirection == LibraryFilterDirection.ASC) {
            compareValuesBy(a, b, selector)
        } else {
            compareValuesBy(b, a, selector)
        }
    }
