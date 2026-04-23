package com.flixclusive.feature.mobile.home

import androidx.compose.runtime.Stable
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.common.domain.Async
import com.flixclusive.core.common.domain.PagingState
import com.flixclusive.core.database.entity.film.DBFilm
import com.flixclusive.core.database.entity.watched.EpisodeProgress
import com.flixclusive.core.database.entity.watched.EpisodeProgressWithMetadata
import com.flixclusive.core.database.entity.watched.MovieProgress
import com.flixclusive.core.database.entity.watched.WatchProgressWithMetadata
import com.flixclusive.core.database.entity.watched.WatchStatus
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.core.datastore.model.user.UiPreferences
import com.flixclusive.core.datastore.model.user.UserPreferences
import com.flixclusive.data.database.repository.LibrarySort
import com.flixclusive.data.database.repository.WatchProgressRepository
import com.flixclusive.data.provider.repository.ProviderRepository
import com.flixclusive.domain.catalog.usecase.GetCatalogItemsUseCase
import com.flixclusive.domain.catalog.usecase.GetHomeCatalogsUseCase
import com.flixclusive.domain.provider.usecase.get.GetCatalogProvidersUseCase
import com.flixclusive.domain.provider.usecase.get.GetFilmMetadataUseCase
import com.flixclusive.domain.provider.usecase.get.GetNextEpisodeUseCase
import com.flixclusive.model.film.Film
import com.flixclusive.model.film.FilmMetadata
import com.flixclusive.model.film.TvShow
import com.flixclusive.model.provider.Catalog
import com.flixclusive.model.provider.ProviderStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

private const val MAX_PAGINATION_PAGES = 5

@HiltViewModel
internal class HomeScreenViewModel @Inject constructor(
    dataStoreManager: DataStoreManager,
    getCatalogProviders: GetCatalogProvidersUseCase,
    private val appDispatchers: AppDispatchers,
    private val getCatalogItems: GetCatalogItemsUseCase,
    private val getFilmMetadata: GetFilmMetadataUseCase,
    private val getHomeCatalogs: GetHomeCatalogsUseCase,
    private val getNextEpisode: GetNextEpisodeUseCase,
    private val providerRepository: ProviderRepository,
    private val userSessionDataStore: UserSessionDataStore,
    private val watchProgressRepository: WatchProgressRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    /** Cache to store if a film has metadata or not to avoid redundant API queries */
    private val cachedFilmMetadata = HashMap<DBFilm, TvShow>()

    private var observeCatalogsJob: Job? = null
    private var loadFetchHeaderJob: Job? = null
    private var toggleJob: Job? = null

    /** Map of jobs for each row catalog loaded on the home screen */
    private val paginationJobs = HashMap<String, Job?>()

    /** Items to display for continue watching section */
    val continueWatchingItems = userSessionDataStore.currentUserId
        .filterNotNull()
        .flatMapLatest { userId ->
            watchProgressRepository.getAllAsFlow(
                ownerId = userId,
                sort = LibrarySort.Modified(ascending = false),
            ).mapLatest { list ->
                list.mapNotNull { item -> filterContinueWatching(item) }
            }
        }.stateIn(
            scope = appDispatchers.ioScope,
            started = SharingStarted.Lazily,
            initialValue = emptyList(),
        )


    /** Displays the title of the media under the card */
    val showFilmTitles = dataStoreManager
        .getUserPrefs(UserPreferences.UI_PREFS_KEY, UiPreferences::class)
        .mapLatest { it.shouldShowTitleOnCards }
        .distinctUntilChanged()
        .stateIn(
            scope = appDispatchers.defaultScope,
            started = SharingStarted.Lazily,
            initialValue = false,
        )

    val catalogProviders = getCatalogProviders()
        .mapLatest {
            if (it !is Async.Success) {
                @Suppress("UNCHECKED_CAST")
                return@mapLatest it as Async<List<CatalogProviderWrapper>>
            }

            val providers = it.data.fastMap { provider ->
                CatalogProviderWrapper(
                    id = provider.id,
                    name = provider.name ?: "--",
                    logoUrl = provider.logoUrl,
                    isEnabled = provider.isEnabled,
                    versionName = provider.versionName ?: "--",
                    versionCode = provider.versionCode ?: 0L,
                    status = provider.status ?: ProviderStatus.Working,
                )
            }

            Async.Success(providers) as Async<List<CatalogProviderWrapper>>
        }
        .stateIn(
            scope = appDispatchers.ioScope,
            started = SharingStarted.Lazily,
            initialValue = Async.Loading,
        )

    init {
        initialize()
    }

    /**
     * Filters the continue watching list to include only items that are not finished.
     *
     * For TV shows, if the episode is finished, it fetches the next episode and adds it to the continue watching list
     * with 0 progress. If there are no more episodes, it excludes the item from the list.
     * */
    private suspend fun filterContinueWatching(item: WatchProgressWithMetadata): WatchProgressWithMetadata? {
        return when (val data = item.watchData) {
            is EpisodeProgress -> {
                if (!data.isCompleted) {
                    return item // Episode not finished, include in continue watching
                }

                var tvShow: FilmMetadata? = cachedFilmMetadata[item.film]
                if (tvShow == null) {
                    val response = getFilmMetadata(item.film).last()
                    if (response is Async.Success) {
                        cachedFilmMetadata[item.film] = response.data as TvShow
                        tvShow = response.data
                    }
                }

                if (tvShow == null) {
                    throw NullPointerException("Film metadata not found for id: ${item.film.id}")
                }

                // Get next episode
                val nextEpisode = getNextEpisode(
                    tvShow = tvShow as TvShow,
                    season = data.seasonNumber,
                    episode = data.episodeNumber,
                )

                if (nextEpisode == null) {
                    null // No next episode, exclude from continue watching
                } else {
                    EpisodeProgressWithMetadata(
                        film = item.film,
                        watchData = EpisodeProgress(
                            ownerId = data.ownerId,
                            filmId = item.film.id,
                            seasonNumber = nextEpisode.season,
                            episodeNumber = nextEpisode.number,
                            progress = 0L,
                            status = WatchStatus.WATCHING,
                        ),
                    )
                }
            }

            is MovieProgress -> {
                if (!data.isCompleted) {
                    return item
                }

                null // Movie is finished, exclude from continue watching
            }
        }
    }

    private fun observeCatalogs() {
        if (observeCatalogsJob?.isActive == true) {
            return
        }

        observeCatalogsJob = viewModelScope.launch {
            getHomeCatalogs().collect { response ->
                when (response) {
                    is Async.Loading -> {
                        _uiState.update { state ->
                            state.copy(catalogs = Async.Loading)
                        }
                    }

                    is Async.Success -> {
                        val catalogs = response.data
                        val catalogMap = catalogs.associateBy { it.url + it.providerId }
                            .mapValues { entry ->
                                CatalogWithPagingState(
                                    catalog = entry.value,
                                    page = 1,
                                    state = PagingState.Idle,
                                    films = emptyList(),
                                )
                            }

                        catalogMap.forEach { (_, data) ->
                            paginate(data)
                        }

                        _uiState.update { state ->
                            state.copy(catalogs = Async.Success(catalogMap))
                        }
                    }

                    is Async.Failure -> {
                        if (response.cause is CancellationException) return@collect

                        _uiState.update { state ->
                            state.copy(catalogs = Async.Failure(response.message))
                        }
                    }
                }
            }
        }
    }

    private fun loadHeaderItem() {
        if (loadFetchHeaderJob?.isActive == true) {
            loadFetchHeaderJob?.cancel()
        }

        loadFetchHeaderJob = viewModelScope.launch {
            val response = _uiState.map { it.catalogs }
                .distinctUntilChanged()
                .first {
                    if (it.isFailure) return@first true

                    val isSuccessButEmpty = it is Async.Success && it.data.isEmpty()
                    if (isSuccessButEmpty) return@first true

                    it is Async.Success && it.data.any { entry -> entry.value.films.isNotEmpty() }
                }

            val catalogs = (response as? Async.Success)?.data?.values ?: emptyList()
            if (catalogs.isEmpty()) return@launch

            val maxRetries = 5
            repeat(maxRetries) { i ->
                val randomCatalog = catalogs.randomOrNull() ?: return@launch
                val randomFilm = randomCatalog.films.randomOrNull() ?: return@launch

                when (val response = getFilmMetadata(randomFilm).last()) {
                    is Async.Success -> {
                        _uiState.update { state ->
                            state.copy(itemHeader = Async.Success(response.data))
                        }
                        return@launch
                    }

                    is Async.Failure -> {
                        if (i != maxRetries - 1) return@repeat
                        if (response.cause is CancellationException) return@launch

                        _uiState.update { state ->
                            state.copy(
                                itemHeader = Async.Success(randomFilm)
                            )
                        }
                    }

                    is Async.Loading -> {
                        _uiState.update { state ->
                            state.copy(itemHeader = Async.Loading)
                        }
                    }
                }
            }
        }
    }

    fun initialize() {
        observeCatalogs()
        loadHeaderItem()
    }

    fun paginate(catalogWithState: CatalogWithPagingState) {
        if (paginationJobs[catalogWithState.key]?.isActive == true || catalogWithState.state.isExhausted) {
            return
        }

        paginationJobs[catalogWithState.key] = viewModelScope.launch {
            val page = catalogWithState.page
            val catalog = catalogWithState.catalog
            getCatalogItems(catalog = catalog, page = page).collect { response ->
                when (response) {
                    is Async.Loading -> {
                        _uiState.update { state ->
                            state.updateCatalog(
                                key = catalogWithState.key,
                                newData = catalogWithState.copy(state = PagingState.Loading)
                            )
                        }
                    }

                    is Async.Success -> {
                        val maxPage = minOf(MAX_PAGINATION_PAGES, response.data.totalPages)
                        val hasNext = page < maxPage && catalogWithState.canPaginate

                        _uiState.update { state ->
                            state.updateCatalog(
                                key = catalogWithState.key,
                                newData = catalogWithState.copy(
                                    state = if (hasNext) PagingState.Idle else PagingState.Exhausted,
                                    films = catalogWithState.films + response.data.results,
                                )
                            )
                        }
                    }

                    is Async.Failure -> {
                        _uiState.update { state ->
                            state.updateCatalog(
                                key = catalogWithState.key,
                                newData = catalogWithState.copy(state = PagingState.Error(response.message))
                            )
                        }
                    }
                }
            }
        }
    }

    fun toggleProvider(id: String) {
        if (toggleJob?.isActive == true) return

        toggleJob = appDispatchers.ioScope.launch {
            val userId = userSessionDataStore.currentUserId.filterNotNull().first()
            providerRepository.toggleProvider(id = id, ownerId = userId)
        }
    }
}

@Stable
internal data class HomeUiState(
    val itemHeader: Async<Film> = Async.Loading,
    val catalogs: Async<Map<String, CatalogWithPagingState>> = Async.Loading,
) {
    fun updateCatalog(
        key: String,
        newData: CatalogWithPagingState,
    ): HomeUiState {
        val currentItems = (catalogs as? Async.Success)?.data ?: emptyMap()
        val updatedItems = currentItems.toMutableMap().apply {
            put(key, newData)
        }

        return copy(catalogs = Async.Success(updatedItems.toMap()))
    }
}

@Stable
internal data class CatalogProviderWrapper(
    val id: String,
    val name: String,
    val logoUrl: String?,
    val isEnabled: Boolean,
    val versionName: String,
    val versionCode: Long,
    val status: ProviderStatus
)

@Stable
internal data class CatalogWithPagingState(
    val catalog: Catalog,
    val page: Int,
    val state: PagingState,
    val films: List<Film>,
) {
    val canPaginate: Boolean get() = catalog.canPaginate
    val url: String get() = catalog.url
    val providerId: String get() = catalog.providerId

    val key: String get() = url + providerId
}
