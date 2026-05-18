package com.flixclusive.mobile

import androidx.compose.runtime.Stable
import androidx.compose.ui.util.fastFilteredMap
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.BuildConfig
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.common.domain.Async
import com.flixclusive.core.common.provider.LoadLinksState
import com.flixclusive.core.common.provider.ProviderWithThrowable
import com.flixclusive.core.database.entity.watched.EpisodeProgressWithMetadata
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.core.datastore.model.user.ProviderPreferences
import com.flixclusive.core.datastore.model.user.UserPreferences
import com.flixclusive.core.network.monitor.NetworkMonitor
import com.flixclusive.core.presentation.player.PlayerCache
import com.flixclusive.core.util.log.infoLog
import com.flixclusive.core.util.webview.WebViewDriverManager
import com.flixclusive.data.database.repository.WatchProgressRepository
import com.flixclusive.data.provider.repository.MediaLinksCacheKey.Companion.toCacheKey
import com.flixclusive.data.provider.repository.MediaLinksRepository
import com.flixclusive.domain.provider.usecase.get.GetMediaLinksUseCase
import com.flixclusive.domain.provider.usecase.get.GetMediaMetadataUseCase
import com.flixclusive.domain.provider.usecase.get.GetNextEpisodeUseCase
import com.flixclusive.domain.provider.usecase.manage.InitializeProvidersUseCase
import com.flixclusive.domain.provider.usecase.manage.ProviderResult
import com.flixclusive.domain.provider.usecase.updater.CheckOutdatedProviderResult
import com.flixclusive.domain.provider.usecase.updater.CheckOutdatedProviderUseCase
import com.flixclusive.domain.provider.usecase.updater.UpdateProviderUseCase
import com.flixclusive.model.media.MediaMetadata
import com.flixclusive.model.media.PartialMedia
import com.flixclusive.model.media.Show
import com.flixclusive.model.media.common.tv.Episode
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
import kotlinx.coroutines.flow.last
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
    private val _getMediaMetadata: GetMediaMetadataUseCase,
    private val getNextEpisode: GetNextEpisodeUseCase,
    private val getMediaLinks: GetMediaLinksUseCase,
    private val watchProgressRepository: WatchProgressRepository,
    private val dataStoreManager: DataStoreManager,
    private val userSessionDataStore: UserSessionDataStore,
    private val appDispatchers: AppDispatchers,
    private val playerCache: PlayerCache,
    private val mediaLinksRepository: MediaLinksRepository,
    private val initializeProviders: InitializeProvidersUseCase,
    private val checkOutdatedProviders: CheckOutdatedProviderUseCase,
    private val updateProvider: UpdateProviderUseCase,
    networkMonitor: NetworkMonitor,
) : ViewModel() {
    private var onFetchMediaLinksJob: Job? = null

    private val _uiState = MutableStateFlow(MobileAppUiState())
    val uiState: StateFlow<MobileAppUiState> = _uiState.asStateFlow()

    private val _providerUpdateInfo = MutableSharedFlow<ProviderUpdateInfo?>()
    val providerUpdateInfo = _providerUpdateInfo.asSharedFlow()

    val currentObservableLinks = mediaLinksRepository.currentObservable

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
            val userId = userSessionDataStore.currentUserId.filterNotNull().first()

            // Ensure that the onboarding process has been completed before loading providers for the first time
            dataStoreManager.getSystemPrefs().first { prefs -> !prefs.isFirstTimeUserLaunch }

            infoLog("Loading $userId's providers for the first time...")
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

    fun onFetchMediaLinks(
        media: MediaMetadata,
        episode: Episode? = null,
    ) {
        if (onFetchMediaLinksJob?.isActive == true && _uiState.value.playerData != null) return

        onFetchMediaLinksJob?.cancel()
        onFetchMediaLinksJob = viewModelScope.launch {
            _uiState.update { it.copy(playerData = PlayerData(media, episode)) }
            mediaLinksRepository.setCurrentObservable(null)
            updateLoadLinksState(LoadLinksState.Fetching(LocaleR.string.media_data_fetching))

            val metadata = getMediaMetadata(media = media)
            if (metadata == null) {
                updateLoadLinksState(LoadLinksState.Error(LocaleR.string.media_data_fetch_failed))
                return@launch
            }

            // Data to be passed to the player screen
            var playerData = PlayerData(media = metadata)

            var episodeToLoad = episode
            if (metadata is Show) {
                if (episode == null) {
                    episodeToLoad = getEpisodeToWatch(tvShow = metadata)
                }

                if (episodeToLoad == null) {
                    updateLoadLinksState(LoadLinksState.Error(LocaleR.string.failed_to_load_episode))
                    return@launch
                }

                playerData = playerData.copy(episode = episodeToLoad)
            }

            val response = getMediaLinks(
                media = metadata,
                episode = episodeToLoad,
            )

            _uiState.update { it.copy(playerData = playerData) }
            response.collect(::updateLoadLinksState)

            if (isFailureButHasLinks()) {
                mediaLinksRepository.setCurrentObservable(null)
            }
        }
    }

    /**
     * Gets a detailed metadata of a non-detailed [MediaMetadata].
     *
     * This assumes that [MediaMetadata] could be a search item.
     * */
    private suspend fun getMediaMetadata(media: MediaMetadata): MediaMetadata? {
        if (media !is PartialMedia) return media

        return when (val response = _getMediaMetadata(media = media).last()) {
            is Async.Success -> response.data
            else -> null
        }
    }

    private suspend fun getEpisodeToWatch(tvShow: Show): Episode? {
        val userId = userSessionDataStore.currentUserId.filterNotNull().first()
        val progress = watchProgressRepository.get(
            id = tvShow.id,
            ownerId = userId,
            type = tvShow.type,
        ) as? EpisodeProgressWithMetadata

        if (progress?.watchData?.isCompleted == true) {
            return getNextEpisode(
                show = tvShow,
                season = progress.watchData.seasonNumber,
                episode = progress.watchData.episodeNumber,
            )
        }

        // Default to 1 if this has not been saved yet
        val seasonNumber = progress?.watchData?.seasonNumber ?: 1
        val episodeNumber = progress?.watchData?.episodeNumber ?: 1


        val seasonIndex = tvShow.seasons.binarySearch {
            it.number.compareTo(seasonNumber)
        }

        val season = tvShow.seasons.getOrNull(seasonIndex)

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

    fun onReleasePlayerCache() {
        appDispatchers.ioScope.launch {
            playerCache.release()
        }
    }

    fun updateLoadLinksState(state: LoadLinksState) {
        val playerData = _uiState.value.playerData
        if (state.hasProviderId && playerData != null) {
            val cache = state.toCacheKey(
                mediaId = playerData.media.id,
                episode = playerData.episode,
            )

            if (cache != null) {
                mediaLinksRepository.setCurrentObservable(cache)
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
        val currentCache = currentObservableLinks.value
        val loadLinksState = _uiState.value.loadLinksState

        return loadLinksState.isError
            && currentCache != null
            && currentCache.hasStreamableLinks
    }
}

@Stable
internal data class MobileAppUiState(
    val loadLinksState: LoadLinksState = LoadLinksState.Idle,
    val playerData: PlayerData? = null,
    val isLoadingProviders: Boolean = false,
    val providerErrors: Map<String, ProviderWithThrowable> = emptyMap(),
)

@Stable
internal data class MediaPreview(
    val media: MediaMetadata,
    val isInLibrary: Boolean,
)

@Stable
internal data class PlayerData(
    val media: MediaMetadata,
    val episode: Episode? = null,
)
