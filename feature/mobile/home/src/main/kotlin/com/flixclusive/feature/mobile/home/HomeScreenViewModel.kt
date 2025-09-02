package com.flixclusive.feature.mobile.home

import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.common.locale.UiText
import com.flixclusive.core.common.pagination.PagingDataState
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
import com.flixclusive.model.film.TvShow
import com.flixclusive.model.provider.Catalog
import dagger.hilt.android.lifecycle.HiltViewModel
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
        private val getHomeHeader: GetHomeHeaderUseCase,
        private val paginateItems: PaginateItemsUseCase,
        private val getEpisode: GetEpisodeUseCase,
        private val getFilmMetadata: GetFilmMetadataUseCase,
        private val dataStoreManager: DataStoreManager,
        private val watchProgressRepository: WatchProgressRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(HomeUiState())
        val uiState = _uiState.asStateFlow()

        /** Displays the title of the media under the card */
        val showFilmTitle = dataStoreManager
            .getUserPrefs(UserPreferences.UI_PREFS_KEY, UiPreferences::class)
            .mapLatest { it.shouldShowTitleOnCards }
            .distinctUntilChanged()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = UiPreferences(),
            )

        /** Map of jobs for each row catalog loaded on the home screen */
        private val paginationJobs = HashMap<String, Job?>()

        init {
            loadHomeHeader()
        }

        /** List of catalogs to display on the home screen */
        val catalogs = getHomeCatalogs()
            .onEach { list ->
                val items = HashMap<String, List<Film>>()
                val pagingStates = HashMap<String, CatalogPagingState>()
                paginationJobs.clear()

                list.forEach { catalog ->
                    items[catalog.url] = mutableStateListOf()
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
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
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
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
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

                    val tvShow = getFilmMetadata(item.film).data
                        ?: throw NullPointerException("Film metadata not found for id: ${item.film.id}")

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
        private fun loadHomeHeader() {
            viewModelScope.launch {
                when (val result = getHomeHeader()) {
                    is Resource.Failure -> {
                        _uiState.update {
                            it.copy(error = result.error)
                        }
                    }
                    Resource.Loading -> Unit
                    is Resource.Success<*> -> {
                        _uiState.update {
                            it.copy(itemHeader = result.data)
                        }
                    }
                }
            }
        }

        fun paginate(catalog: Catalog, page: Int) {
            if(paginationJobs[catalog.url]?.isActive == true)
                return

            paginationJobs[catalog.url] = viewModelScope.launch {
                _uiState.update {
                    val pagingState = it.pagingStates[catalog.url]
                        ?: return@update it

                    it.updatePagingState(
                        key = catalog.url,
                        newState = pagingState.copy(state = PagingDataState.Loading)
                    )
                }

                val response = paginateItems(catalog = catalog, page = page)
                val data = response.data

                if (data == null) {
                    // TODO: Handle error state
                    return@launch
                }

                if (response is Resource.Failure) {
                    // TODO: Handle error state
                    return@launch
                }

                val maxPage = minOf(MAX_PAGINATION_PAGES, data.totalPages)
                val hasNext = data.results.size == 20 && page < maxPage && catalog.canPaginate

                _uiState.update {
                    val items = response.data?.results ?: emptyList()
                    it.addItems(key = catalog.url, newItems = items)
                        .updatePagingState(
                            key = catalog.url,
                            newState = CatalogPagingState(
                                hasNext = hasNext,
                                state = when {
                                    hasNext -> PagingDataState.Loading
                                    else -> PagingDataState.Error(LocaleR.string.end_of_list)
                                },
                                page = when {
                                    hasNext -> page + 1
                                    else -> page
                                },
                            )
                        )
                }
            }
        }
    }

@Stable
internal data class HomeUiState(
    val itemHeader: Film? = null,
    val items: Map<String, List<Film>> = HashMap(),
    val pagingStates: Map<String, CatalogPagingState> = HashMap(),
    val error: UiText? = null,
) {
    companion object {
        const val MAX_PAGINATION_PAGES = 5

        fun HomeUiState.addItems(key: String, newItems: List<Film>): HomeUiState {
            val updatedItems = items.toMutableMap()
            val currentList = updatedItems[key]?.toMutableList() ?: mutableListOf()

            currentList.addAll(newItems)
            updatedItems[key] = currentList

            return this.copy(items = updatedItems.toMap())
        }

        fun HomeUiState.updatePagingState(key: String, newState: CatalogPagingState): HomeUiState {
            val updatedStates = pagingStates.toMutableMap()
            updatedStates[key] = newState

            return this.copy(pagingStates = updatedStates.toMap())
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
