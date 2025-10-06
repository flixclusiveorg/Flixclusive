package com.flixclusive.feature.mobile.home

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.common.locale.UiText
import com.flixclusive.core.common.pagination.PagingDataState
import com.flixclusive.core.database.entity.film.DBFilm
import com.flixclusive.core.database.entity.watched.EpisodeProgress
import com.flixclusive.core.database.entity.watched.MovieProgress
import com.flixclusive.core.database.entity.watched.WatchProgressWithMetadata
import com.flixclusive.core.database.entity.watched.WatchStatus
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.datastore.model.user.UiPreferences
import com.flixclusive.core.datastore.model.user.UserPreferences
import com.flixclusive.core.network.util.Resource
import com.flixclusive.data.database.repository.WatchProgressRepository
import com.flixclusive.data.database.session.UserSessionManager
import com.flixclusive.domain.catalog.usecase.GetHomeCatalogsUseCase
import com.flixclusive.domain.catalog.usecase.GetHomeHeaderUseCase
import com.flixclusive.domain.catalog.usecase.PaginateItemsUseCase
import com.flixclusive.domain.provider.usecase.get.GetEpisodeUseCase
import com.flixclusive.domain.provider.usecase.get.GetFilmMetadataUseCase
import com.flixclusive.feature.mobile.home.HomeUiState.Companion.MAX_PAGINATION_PAGES
import com.flixclusive.feature.mobile.home.HomeUiState.Companion.addItems
import com.flixclusive.feature.mobile.home.HomeUiState.Companion.updatePagingState
import com.flixclusive.model.film.Film
import com.flixclusive.model.film.FilmMetadata
import com.flixclusive.model.film.TvShow
import com.flixclusive.model.provider.Catalog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentHashMapOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentHashMap
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.flixclusive.core.strings.R as LocaleR

@HiltViewModel
internal class HomeScreenViewModel
    @Inject
    constructor(
        getHomeCatalogs: GetHomeCatalogsUseCase,
        userSessionManager: UserSessionManager,
        appDispatchers: AppDispatchers,
        dataStoreManager: DataStoreManager,
        private val getHomeHeader: GetHomeHeaderUseCase,
        private val paginateItems: PaginateItemsUseCase,
        private val getEpisode: GetEpisodeUseCase,
        private val getFilmMetadata: GetFilmMetadataUseCase,
        private val watchProgressRepository: WatchProgressRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(HomeUiState())
        val uiState = _uiState.asStateFlow()

        /** Cache to store if a film has metadata or not to avoid redundant API queries */
        private val cachedFilmMetadata = HashMap<DBFilm, TvShow>()

        /** Displays the title of the media under the card */
        val showFilmTitles = dataStoreManager
            .getUserPrefs(UserPreferences.UI_PREFS_KEY, UiPreferences::class)
            .mapLatest { it.shouldShowTitleOnCards }
            .distinctUntilChanged()
            .stateIn(
                scope = appDispatchers.defaultScope,
                started = SharingStarted.Eagerly,
                initialValue = false,
            )

        /** Map of jobs for each row catalog loaded on the home screen */
        private val paginationJobs = HashMap<String, Job?>()

        init {
            loadHomeHeader()
        }

        /** List of catalogs to display on the home screen */
        val catalogs = getHomeCatalogs()
            .onEach { list ->
                val items = HashMap<String, PersistentSet<Film>>()
                val pagingStates = HashMap<String, CatalogPagingState>()
                paginationJobs.clear()

                list.forEach { catalog ->
                    items[catalog.url] = persistentSetOf<Film>()
                    pagingStates[catalog.url] = CatalogPagingState(
                        hasNext = catalog.canPaginate,
                        state = when {
                            !catalog.canPaginate -> PagingDataState.Error(LocaleR.string.end_of_list)
                            else -> PagingDataState.Loading
                        },
                        page = 1,
                    )

                    paginationJobs[catalog.url] = null
                }

                _uiState.update {
                    it.copy(
                        items = items.toPersistentHashMap(),
                        pagingStates = pagingStates.toPersistentHashMap(),
                    )
                }
            }.stateIn(
                scope = appDispatchers.defaultScope,
                started = SharingStarted.Eagerly,
                initialValue = emptyList(),
            )

        /** Items to display for continue watching section */
        val continueWatchingItems = userSessionManager.currentUser
            .filterNotNull()
            .flatMapLatest { user ->
                watchProgressRepository.getAllAsFlow(ownerId = user.id)
            }.mapLatest { list ->
                list.mapNotNull { item -> filterContinueWatching(item) }
            }.stateIn(
                scope = appDispatchers.ioScope,
                started = SharingStarted.Eagerly,
                initialValue = emptyList(),
            )

        /**
         * Filters the continue watching list to include only items that are not finished.
         *
         * For TV shows, if the episode is finished, it fetches the next episode and adds it to the continue watching list
         * with 0 progress. If there are no more episodes, it excludes the item from the list.
         * */
        private suspend fun filterContinueWatching(item: WatchProgressWithMetadata): WatchProgressWithMetadata? {
            return when (val data = item.watchData) {
                is EpisodeProgress -> {
                    if (!data.isFinished) {
                        return item // Episode not finished, include in continue watching
                    }

                    var tvShow: FilmMetadata? = cachedFilmMetadata[item.film]

                    if (tvShow == null) {
                        tvShow = getFilmMetadata(item.film).data?.also {
                            cachedFilmMetadata[item.film] = it as TvShow
                        }
                    }

                    if (tvShow == null) {
                        throw NullPointerException("Film metadata not found for id: ${item.film.id}")
                    }

                    // Get next episode
                    val nextEpisode = getEpisode(
                        tvShow = tvShow as TvShow,
                        season = data.seasonNumber,
                        episode = data.episodeNumber + 1,
                    )

                    if (nextEpisode == null) {
                        null // No next episode, exclude from continue watching
                    } else {
                        // THIS IS BAD CODE BUT I'M SICK OF REWRITING THE WHOLE WATCH PROGRESS SYSTEM

                        // Insert next episode progress with 0 progress and return it
                        val id = watchProgressRepository.insert(
                            item = EpisodeProgress(
                                ownerId = data.ownerId,
                                filmId = item.film.id,
                                seasonNumber = nextEpisode.season,
                                episodeNumber = nextEpisode.number,
                                progress = 0L,
                                status = WatchStatus.WATCHING,
                            ),
                            film = item.film,
                        )

                        // Return newly created episode progress with metadata
                        watchProgressRepository.get(
                            id = id,
                            type = item.film.filmType,
                        )
                    }
                }

                is MovieProgress -> {
                    if (!data.isFinished) {
                        return item
                    }

                    null // Movie is finished, exclude from continue watching
                }
            }
        }

        /**
         * Loads the header item for the home screen.
         * */
        fun loadHomeHeader() {
            viewModelScope.launch {
                _uiState.update {
                    it.copy(itemHeaderError = null)
                }

                when (val result = getHomeHeader()) {
                    is Resource.Failure -> {
                        _uiState.update {
                            it.copy(itemHeaderError = result.error)
                        }
                    }

                    Resource.Loading -> Unit
                    is Resource.Success<*> -> {
                        _uiState.update {
                            it.copy(
                                itemHeader = result.data,
                                itemHeaderError = null,
                            )
                        }
                    }
                }
            }
        }

        fun paginate(catalog: Catalog) {
            if (paginationJobs[catalog.url]?.isActive == true) {
                return
            }

            paginationJobs[catalog.url] = viewModelScope.launch {
                _uiState.update {
                    val pagingState = it.pagingStates[catalog.url]
                        ?: return@update it

                    it.updatePagingState(
                        key = catalog.url,
                        newState = pagingState.copy(state = PagingDataState.Loading),
                    )
                }

                val page = _uiState.value.pagingStates[catalog.url]!!.page
                val response = paginateItems(catalog = catalog, page = page)
                val data = response.data

                if (data == null || response is Resource.Failure) {
                    _uiState.update {
                        val oldPagingState = it.pagingStates[catalog.url] ?: return@launch

                        val errorState = when (response) {
                            is Resource.Failure -> PagingDataState.Error(response.error!!)
                            else -> PagingDataState.Error()
                        }

                        it.updatePagingState(
                            key = catalog.url,
                            newState = oldPagingState.copy(state = errorState),
                        )
                    }
                    return@launch
                }

                val maxPage = minOf(MAX_PAGINATION_PAGES, data.totalPages)
                val hasNext = page < maxPage && catalog.canPaginate

                _uiState.update {
                    val items = response.data?.results ?: emptyList()
                    it
                        .addItems(key = catalog.url, newItems = items)
                        .updatePagingState(
                            key = catalog.url,
                            newState = CatalogPagingState(
                                hasNext = hasNext,
                                page = page + 1, // Add 1 to the current page
                                state = PagingDataState.Success(isExhausted = !hasNext),
                            ),
                        )
                }
            }
        }
    }

@Stable
internal data class HomeUiState(
    val itemHeader: Film? = null,
    val itemHeaderError: UiText? = null,
    val items: PersistentMap<String, PersistentSet<Film>> = persistentHashMapOf(),
    val pagingStates: PersistentMap<String, CatalogPagingState> = persistentHashMapOf(),
) {
    companion object {
        const val MAX_PAGINATION_PAGES = 5

        fun HomeUiState.addItems(
            key: String,
            newItems: List<Film>,
        ): HomeUiState {
            val currentItems = items[key] ?: return this

            return copy(items = items.put(key, currentItems.addAll(newItems)))
        }

        fun HomeUiState.updatePagingState(
            key: String,
            newState: CatalogPagingState,
        ): HomeUiState {
            if (!pagingStates.containsKey(key)) return this

            return copy(pagingStates = pagingStates.put(key, newState))
        }
    }
}

/**
 * Holds information about the pagination state for a specific catalog.
 * */
@Stable
internal data class CatalogPagingState(
    val hasNext: Boolean,
    val page: Int,
    val state: PagingDataState,
)
