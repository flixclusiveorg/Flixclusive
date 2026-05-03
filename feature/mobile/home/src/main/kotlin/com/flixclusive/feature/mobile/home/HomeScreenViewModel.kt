package com.flixclusive.feature.mobile.home

import androidx.compose.runtime.Stable
import androidx.compose.ui.util.fastMapNotNull
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.common.domain.Async
import com.flixclusive.core.common.domain.PagingState
import com.flixclusive.core.database.entity.media.DBMedia
import com.flixclusive.core.database.entity.media.DBMedia.Companion.toMediaMetadata
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
import com.flixclusive.domain.catalog.usecase.GetCatalogItemsUseCase
import com.flixclusive.domain.catalog.usecase.GetHomeCatalogsUseCase
import com.flixclusive.domain.provider.usecase.get.GetCatalogProvidersUseCase
import com.flixclusive.domain.provider.usecase.get.GetMediaMetadataUseCase
import com.flixclusive.domain.provider.usecase.get.GetNextEpisodeUseCase
import com.flixclusive.domain.provider.usecase.manage.ToggleProviderUseCase
import com.flixclusive.model.media.MediaMetadata
import com.flixclusive.model.media.PartialMedia
import com.flixclusive.model.media.Show
import com.flixclusive.model.provider.Catalog
import com.flixclusive.model.provider.ProviderMetadata
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
    userSessionDataStore: UserSessionDataStore,
    appDispatchers: AppDispatchers,
    private val getCatalogItems: GetCatalogItemsUseCase,
    private val getMediaMetadata: GetMediaMetadataUseCase,
    private val getHomeCatalogs: GetHomeCatalogsUseCase,
    private val getNextEpisode: GetNextEpisodeUseCase,
    private val watchProgressRepository: WatchProgressRepository,
    private val toggleProvider: ToggleProviderUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    /** Cache to store if a media has metadata or not to avoid redundant API queries */
    private val cachedMediaMetadata = HashMap<DBMedia, Show>()

    private var observeCatalogsJob: Job? = null
    private var loadFetchHeaderJob: Job? = null

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
    val showMediaTitles = dataStoreManager
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
                return@mapLatest it as Async<List<CatalogProvider>>
            }

            val providers = it.data.fastMapNotNull { provider ->
                CatalogProvider(
                    provider = provider.metadata ?: return@fastMapNotNull null,
                    isEnabled = provider.isEnabled,
                )
            }

            Async.Success(providers) as Async<List<CatalogProvider>>
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

                var tvShow: MediaMetadata? = cachedMediaMetadata[item.media]
                if (tvShow == null) {
                    val response = getMediaMetadata(item.media.toMediaMetadata()).last()
                    if (response is Async.Success) {
                        cachedMediaMetadata[item.media] = response.data as Show
                        tvShow = response.data
                    }
                }

                if (tvShow == null) {
                    throw NullPointerException("MediaMetadata metadata not found for id: ${item.media.id}")
                }

                // Get next episode
                val nextEpisode = getNextEpisode(
                    show = tvShow as Show,
                    season = data.seasonNumber,
                    episode = data.episodeNumber,
                )

                if (nextEpisode == null) {
                    null // No next episode, exclude from continue watching
                } else {
                    EpisodeProgressWithMetadata(
                        media = item.media,
                        watchData = EpisodeProgress(
                            ownerId = data.ownerId,
                            mediaId = item.media.id,
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
                                    medias = emptyList(),
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

                    it is Async.Success && it.data.any { entry -> entry.value.medias.isNotEmpty() }
                }

            val catalogs = (response as? Async.Success)?.data?.values ?: emptyList()
            if (catalogs.isEmpty()) return@launch

            val maxRetries = 5
            repeat(maxRetries) { i ->
                val randomCatalog = catalogs.randomOrNull() ?: return@launch
                val randomMedia = randomCatalog.medias.randomOrNull() ?: return@launch

                when (val response = getMediaMetadata(randomMedia).last()) {
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
                                itemHeader = Async.Success(randomMedia)
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
                        val hasNext = page < MAX_PAGINATION_PAGES
                            && response.data.hasNextPage
                            && catalogWithState.canPaginate

                        _uiState.update { state ->
                            state.updateCatalog(
                                key = catalogWithState.key,
                                newData = catalogWithState.copy(
                                    state = if (hasNext) PagingState.Idle else PagingState.Exhausted,
                                    medias = catalogWithState.medias + response.data.results,
                                    page = catalogWithState.page + 1,
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

    fun onToggleProvider(id: String) {
        toggleProvider(id)
    }
}

@Stable
internal data class HomeUiState(
    val itemHeader: Async<MediaMetadata> = Async.Loading,
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
internal data class CatalogProvider(
    val provider: ProviderMetadata,
    val isEnabled: Boolean,
) {
    val id: String get() = provider.id
    val name: String get() = provider.name
    val iconUrl: String? get() = provider.iconUrl
    val versionName: String get() = provider.versionName
    val versionCode: Long get() = provider.versionCode
    val status: ProviderStatus get() = provider.status
}

@Stable
internal data class CatalogWithPagingState(
    val catalog: Catalog,
    val page: Int,
    val state: PagingState,
    val medias: List<PartialMedia>,
) {
    val canPaginate: Boolean get() = catalog.canPaginate
    val url: String get() = catalog.url
    val providerId: String get() = catalog.providerId

    val key: String get() = url + providerId
}
