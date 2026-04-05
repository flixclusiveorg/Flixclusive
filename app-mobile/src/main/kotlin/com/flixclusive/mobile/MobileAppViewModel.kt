package com.flixclusive.mobile

import androidx.compose.runtime.Stable
import androidx.compose.ui.util.fastFilteredMap
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.BuildConfig
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.common.provider.LoadLinksState
import com.flixclusive.core.common.provider.ProviderWithThrowable
import com.flixclusive.core.database.entity.watched.EpisodeProgressWithMetadata
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.datastore.model.user.ProviderPreferences
import com.flixclusive.core.datastore.model.user.UserPreferences
import com.flixclusive.core.network.monitor.NetworkMonitor
import com.flixclusive.core.network.util.Resource
import com.flixclusive.core.presentation.player.PlayerCache
import com.flixclusive.core.util.log.infoLog
import com.flixclusive.core.util.webview.WebViewDriverManager
import com.flixclusive.data.database.repository.LibraryListRepository
import com.flixclusive.data.database.repository.WatchProgressRepository
import com.flixclusive.data.database.session.UserSessionManager
import com.flixclusive.data.provider.repository.CacheKey.Companion.toCacheKey
import com.flixclusive.data.provider.repository.CachedLinksRepository
import com.flixclusive.domain.provider.usecase.get.GetFilmMetadataUseCase
import com.flixclusive.domain.provider.usecase.get.GetMediaLinksUseCase
import com.flixclusive.domain.provider.usecase.get.GetNextEpisodeUseCase
import com.flixclusive.domain.provider.usecase.get.GetSeasonUseCase
import com.flixclusive.domain.provider.usecase.manage.InitializeProvidersUseCase
import com.flixclusive.domain.provider.usecase.manage.ProviderResult
import com.flixclusive.domain.provider.usecase.updater.CheckOutdatedProviderResult
import com.flixclusive.domain.provider.usecase.updater.CheckOutdatedProviderUseCase
import com.flixclusive.domain.provider.usecase.updater.UpdateProviderUseCase
import com.flixclusive.model.film.Film
import com.flixclusive.model.film.FilmMetadata
import com.flixclusive.model.film.Movie
import com.flixclusive.model.film.TvShow
import com.flixclusive.model.film.common.tv.Episode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.flixclusive.core.strings.R as LocaleR

internal sealed class ProviderUpdateInfo {
    data class Updated(val providerNames: List<String>) : ProviderUpdateInfo()
    data class Outdated(val providerNames: List<String>) : ProviderUpdateInfo()
}

@HiltViewModel
internal class MobileAppViewModel @Inject constructor(
    private val _getFilmMetadata: GetFilmMetadataUseCase,
    private val getNextEpisode: GetNextEpisodeUseCase,
    private val getSeason: GetSeasonUseCase,
    private val getMediaLinks: GetMediaLinksUseCase,
    private val watchProgressRepository: WatchProgressRepository,
    private val dataStoreManager: DataStoreManager,
    private val userSessionManager: UserSessionManager,
    private val libraryListRepository: LibraryListRepository,
    private val appDispatchers: AppDispatchers,
    private val playerCache: PlayerCache,
    private val cachedLinksRepository: CachedLinksRepository,
    private val initializeProviders: InitializeProvidersUseCase,
    private val checkOutdatedProviders: CheckOutdatedProviderUseCase,
    private val updateProvider: UpdateProviderUseCase,
    networkMonitor: NetworkMonitor,
) : ViewModel() {
    private var onFilmLongClickJob: Job? = null
    private var onFetchMediaLinksJob: Job? = null

    private val _uiState = MutableStateFlow(MobileAppUiState())
    val uiState: StateFlow<MobileAppUiState> = _uiState.asStateFlow()

    private val _providerUpdateInfo = MutableSharedFlow<ProviderUpdateInfo?>()
    val providerUpdateInfo = _providerUpdateInfo.asSharedFlow()

    val currentLinksCache = cachedLinksRepository.currentCache

    /**
     * A WebView driver instance that is shared across the app.
     *
     * This is initialized by providers that require a WebView to fetch media links.
     * It is destroyed when the user leaves the app or when it's no longer needed to free up resources.
     * */
    val webViewDriver = WebViewDriverManager.webView
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null,
        )

    /**
     * A StateFlow to check if user is connected to the internet.
     * */
    val hasInternet = networkMonitor.isOnline
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = true,
        )

    val hasNotSeenNewChangelogs = dataStoreManager
        .getSystemPrefs()
        .mapLatest { BuildConfig.VERSION_CODE > it.lastSeenChangelogs }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = true,
        )

    init {
        viewModelScope.launch {
            val user = userSessionManager.currentUser.filterNotNull().first().name
            infoLog("Loading $user's providers for the first time...")
            initProviders()
            updateProviders()
        }
    }

    private suspend fun initProviders() {
        initializeProviders()
            .onStart {
                _uiState.update { it.copy(isLoadingProviders = true) }
            }
            .onEach { result ->
                if (result !is ProviderResult.Failure) return@onEach

                _uiState.update { state ->
                    val pair = result.provider.id to ProviderWithThrowable(
                        provider = result.provider,
                        throwable = result.error,
                    )

                    state.copy(providerErrors = state.providerErrors + pair)
                }
            }
            .onCompletion {
                _uiState.update { it.copy(isLoadingProviders = false) }
            }.collect()
    }

    private suspend fun updateProviders() {
        val providerPrefs = dataStoreManager.getUserPrefs(
            key = UserPreferences.PROVIDER_PREFS_KEY,
            type = ProviderPreferences::class
        ).first()

        val outdatedProviders = checkOutdatedProviders()
            .fastFilteredMap(
                predicate = { it is CheckOutdatedProviderResult.Outdated },
                transform = { it.metadata }
            )

        if (outdatedProviders.isEmpty()) return
        if (!providerPrefs.isAutoUpdateEnabled) {
            val names = outdatedProviders.fastMap { it.name }
            _providerUpdateInfo.emit(ProviderUpdateInfo.Outdated(names))
            return
        }

        val results = updateProvider(outdatedProviders)
        if (results.success.isEmpty()) return

        // Remove providers that were updated successfully from the errors list in the ui state
        results.success.forEach {
            _uiState.update { state ->
                state.copy(providerErrors = state.providerErrors - it.id)
            }
        }

        // Add providers that failed to update to the errors list in the ui state
        results.failed.forEach { (provider, throwable) ->
            val pair = provider.id to ProviderWithThrowable(
                provider = provider,
                throwable = throwable ?: Error("Failed to update provider"),
            )

            _uiState.update { state ->
                state.copy(providerErrors = state.providerErrors + pair)
            }
        }

        if (results.success.isNotEmpty()) {
            val names = results.success.fastMap { it.name }
            _providerUpdateInfo.emit(ProviderUpdateInfo.Updated(names))
        }
    }

    fun onConsumeProviderErrors() {
        _uiState.update { state ->
            state.copy(providerErrors = emptyMap())
        }
    }

    fun previewFilm(film: Film) {
        if (onFilmLongClickJob?.isActive == true) return

        onFilmLongClickJob = viewModelScope.launch {
            val userId = userSessionManager.currentUser.filterNotNull().first().id
            val libraryItem = libraryListRepository.getListsContainingFilm(
                filmId = film.identifier,
                ownerId = userId
            ).first()
            val isInLibrary = libraryItem.isNotEmpty()

            _uiState.update {
                it.copy(
                    filmPreviewState = FilmPreview(
                        film = film,
                        isInLibrary = isInLibrary,
                    ),
                )
            }
        }
    }

    fun onFetchMediaLinks(
        film: Film,
        episode: Episode? = null,
    ) {
        if (onFetchMediaLinksJob?.isActive == true && _uiState.value.playerData != null) return

        onFetchMediaLinksJob?.cancel()
        onFetchMediaLinksJob = viewModelScope.launch {
            _uiState.update { it.copy(playerData = PlayerData(film, episode)) }
            cachedLinksRepository.setCurrentCache(null)
            updateLoadLinksState(LoadLinksState.Fetching(LocaleR.string.film_data_fetching))

            val metadata = getFilmMetadata(film = film)
            if (metadata == null) {
                updateLoadLinksState(LoadLinksState.Error(LocaleR.string.film_data_fetch_failed))
                return@launch
            }

            // Data to be passed to the player screen
            var playerData = PlayerData(film = metadata)

            val responseFlow = when (metadata) {
                is Movie -> getMediaLinks(movie = metadata)
                is TvShow -> {
                    var episodeToLoad = episode
                    if (episode == null) {
                        episodeToLoad = getEpisodeToWatch(tvShow = metadata)
                    }

                    if (episodeToLoad == null) {
                        updateLoadLinksState(LoadLinksState.Error(LocaleR.string.failed_to_load_episode))
                        return@launch
                    }

                    playerData = playerData.copy(episode = episodeToLoad)

                    getMediaLinks(
                        tvShow = metadata,
                        episode = episodeToLoad,
                    )
                }

                else -> error("This is not a valid FilmMetadata subclass: $metadata")
            }

            _uiState.update { it.copy(playerData = playerData) }
            responseFlow.collect(::updateLoadLinksState)

            if (isFailureButHasLinks()) {
                cachedLinksRepository.setCurrentCache(null)
            }
        }
    }

    /**
     * Gets a detailed metadata of a non-detailed [Film].
     *
     * This assumes that [Film] could be a search item.
     * */
    private suspend fun getFilmMetadata(film: Film): FilmMetadata? {
        if (film is FilmMetadata) return film

        return when (val response = _getFilmMetadata(film = film)) {
            is Resource.Success -> response.data
            else -> null
        }
    }

    private suspend fun getEpisodeToWatch(tvShow: TvShow): Episode? {
        val userId = userSessionManager.currentUser.filterNotNull().first().id
        val progress = watchProgressRepository.get(
            id = tvShow.identifier,
            ownerId = userId,
            type = tvShow.filmType,
        ) as? EpisodeProgressWithMetadata

        if (progress?.watchData?.isCompleted == true) {
            return getNextEpisode(
                tvShow = tvShow,
                season = progress.watchData.seasonNumber,
                episode = progress.watchData.episodeNumber,
            )
        }

        // Default to 1 if this has not been saved yet
        val seasonNumber = progress?.watchData?.seasonNumber ?: 1
        val episodeNumber = progress?.watchData?.episodeNumber ?: 1

        val season = getSeason(
            tvShow = tvShow,
            number = seasonNumber,
        ).let { response ->
            when (response) {
                is Resource.Success -> response.data
                else -> null
            }
        }

        val episode = season?.episodes?.binarySearch {
            it.number.compareTo(episodeNumber)
        }?.let { index -> season.episodes.getOrNull(index) }

        return episode
    }

    fun hideWebViewDriver() {
        WebViewDriverManager.destroy()
    }

    fun onStopLoadingLinks(isForceClosing: Boolean = false) {
        updateLoadLinksState(LoadLinksState.Idle)
        if (isForceClosing) {
            onFetchMediaLinksJob?.cancel() // Cancel job
        }
    }

    fun onSaveLastSeenChangelogs(version: Long) {
        appDispatchers.ioScope.launch {
            dataStoreManager.updateSystemPrefs {
                it.copy(lastSeenChangelogs = version)
            }
        }
    }

    fun onRemovePreviewFilm() {
        _uiState.update { it.copy(filmPreviewState = null) }
    }

    fun onReleasePlayerCache() {
        appDispatchers.ioScope.launch {
            playerCache.release()
        }
    }

    fun updateLoadLinksState(state: LoadLinksState) {
        val playerData = _uiState.value.playerData
        if (state.hasProviderId && playerData != null) {
            val cache = state.toCacheKey(
                filmId = playerData.film.identifier,
                episode = playerData.episode,
            )

            if (cache != null) {
                cachedLinksRepository.setCurrentCache(cache)
            }
        }

        _uiState.update {
            it.copy(
                loadLinksState = state,
                playerData = if (state.isIdle) null else it.playerData
            )
        }
    }

    private fun isFailureButHasLinks(): Boolean {
        val currentCache = currentLinksCache.value
        val loadLinksState = _uiState.value.loadLinksState

        return loadLinksState.isError
            && currentCache != null
            && currentCache.hasStreamableLinks
    }
}

@Stable
internal data class MobileAppUiState(
    val loadLinksState: LoadLinksState = LoadLinksState.Idle,
    val filmPreviewState: FilmPreview? = null,
    val playerData: PlayerData? = null,
    val isLoadingProviders: Boolean = false,
    val providerErrors: Map<String, ProviderWithThrowable> = emptyMap(),
)

@Stable
internal data class FilmPreview(
    val film: Film,
    val isInLibrary: Boolean,
)

@Stable
internal data class PlayerData(
    val film: Film,
    val episode: Episode? = null,
)
