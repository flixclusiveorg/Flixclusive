package com.flixclusive.core.ui.player

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.datastore.AppSettingsManager
import com.flixclusive.core.ui.player.util.PlayerCacheManager
import com.flixclusive.core.util.common.resource.Resource
import com.flixclusive.core.util.common.ui.UiText
import com.flixclusive.data.watch_history.WatchHistoryRepository
import com.flixclusive.domain.database.WatchTimeUpdaterUseCase
import com.flixclusive.domain.provider.SourceLinksProviderUseCase
import com.flixclusive.domain.tmdb.SeasonProviderUseCase
import com.flixclusive.model.database.WatchHistoryItem
import com.flixclusive.model.database.toWatchHistoryItem
import com.flixclusive.model.database.util.getSavedTimeForFilm
import com.flixclusive.model.provider.SourceData
import com.flixclusive.model.provider.SourceDataState
import com.flixclusive.model.tmdb.Season
import com.flixclusive.model.tmdb.TMDBEpisode
import com.flixclusive.model.tmdb.TvShow
import com.flixclusive.provider.util.FlixclusiveWebView
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import kotlin.math.max
import com.flixclusive.core.util.R as UtilR

abstract class BasePlayerViewModel(
    args: PlayerScreenNavArgs,
    client: OkHttpClient,
    context: Context,
    playerCacheManager: PlayerCacheManager,
    watchHistoryRepository: WatchHistoryRepository,
    private val appSettingsManager: AppSettingsManager,
    private val seasonProviderUseCase: SeasonProviderUseCase,
    private val sourceLinksProvider: SourceLinksProviderUseCase,
    private val watchTimeUpdaterUseCase: WatchTimeUpdaterUseCase,
) : ViewModel() {
    val film = args.film

    val player = FlixclusivePlayerManager(
        client = client,
        context = context,
        playerCacheManager = playerCacheManager,
        appSettings = appSettingsManager.localAppSettings
    )

    val sourceData: SourceData
        get() = sourceLinksProvider.getLinks(
            filmId = film.id,
            episode = _currentSelectedEpisode.value
        )

    val sourceProviders = sourceLinksProvider.providerApis

    private val _dialogState = MutableStateFlow<SourceDataState>(SourceDataState.Idle)
    val dialogState: StateFlow<SourceDataState> = _dialogState.asStateFlow()

    private val _season = MutableStateFlow<Resource<Season?>>(Resource.Loading)
    val season = _season.asStateFlow()

    var areControlsVisible by mutableStateOf(true)
    var areControlsLocked by mutableStateOf(false)

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private val _currentSelectedEpisode = MutableStateFlow(args.episodeToPlay)
    open val currentSelectedEpisode = _currentSelectedEpisode.asStateFlow()

    /**
     * For the next episode to
     * seamlessly watch tv shows
     * */
    private var nextEpisodeToUse: TMDBEpisode? by mutableStateOf(null)
    private var isNextEpisodeLoaded = false
    var isLastEpisode by mutableStateOf(false)
        private set

    val watchHistoryItem = watchHistoryRepository
        .getWatchHistoryItemByIdInFlow(film.id)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = runBlocking { watchHistoryRepository.getWatchHistoryItemById(film.id) ?: film.toWatchHistoryItem() }
        )

    val appSettings = appSettingsManager.appSettings
        .data
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = appSettingsManager.localAppSettings
        )

    private var loadLinksFromNewProviderJob: Job? = null
    private var loadLinksJob: Job? = null
    private var loadNextLinksJob: Job? = null
    private var onSeasonChangeJob: Job? = null

    open fun resetUiState() {
        _uiState.update {
            val preferredServer = appSettings.value.preferredQuality
            val indexOfPreferredServer = sourceData.cachedLinks.indexOfFirst { server ->
                server.name.contains(preferredServer.qualityName, true)
                || server.name.equals(preferredServer.qualityName, true)
            }

            it.copy(
                selectedSourceLink = max(indexOfPreferredServer, 0),
                selectedProvider = sourceData.providerName,
            )
        }
    }

    /**
     *
     * Obtains the saved time for the current [SourceData] to be watched.
     *
     * @param episode an optional parameter for episode to be watched if the film is a [TvShow]
     *
     * @return A pair of time in [Long] format - Pair(watchTime, durationTime)
     * */
    fun getSavedTimeForSourceData(episode: TMDBEpisode? = null): Pair<Long, Long> {
        val episodeToUse = episode ?: currentSelectedEpisode.value

        val watchHistoryItemToUse = watchHistoryItem.value ?: return 0L to 0L

        isLastEpisode = checkIfLastEpisode(episodeToUse, watchHistoryItemToUse)

        return getSavedTimeForFilm(
            watchHistoryItemToUse,
            episodeToUse
        )
    }

    private fun checkIfLastEpisode(
        episodeToCheck: TMDBEpisode?,
        watchHistoryItem: WatchHistoryItem?,
    ): Boolean {
        val episode = episodeToCheck ?: return false
        val lastSeason = watchHistoryItem?.seasons
        val lastEpisode = watchHistoryItem?.episodes?.get(lastSeason)

        return episode.season == lastSeason && episode.episode == lastEpisode
    }

    /**
     *
     * A callback to show an error message to the player screen.
     *
     * @param message the global [UiText] message to be sent and parse by the player
     * */
    protected abstract fun showErrorOnUiCallback(message: UiText)

    fun onSeasonChange(seasonNumber: Int) {
        if (onSeasonChangeJob?.isActive == true)
            return

        onSeasonChangeJob = viewModelScope.launch {
            seasonProviderUseCase.asFlow(
                id = film.id,
                seasonNumber = seasonNumber
            ).collectLatest { _season.value = it }
        }
    }

    fun onProviderChange(
        newProvider: String,
        runWebView: (FlixclusiveWebView) -> Unit
    ) {
        if (loadLinksFromNewProviderJob?.isActive == true)
            return

        // Cancel episode queueing job
        if (loadNextLinksJob?.isActive == true) {
            loadNextLinksJob?.cancel()
            loadNextLinksJob = null
        }

        updateWatchHistory(
            currentTime = player.currentPosition,
            duration = player.duration
        )

        loadLinksFromNewProviderJob = viewModelScope.launch {
            val oldSelectedSource = _uiState.value.selectedProvider
            updateProviderSelected(newProvider)

            sourceLinksProvider.loadLinks(
                film = film,
                preferredProviderName = newProvider,
                isChangingProvider = true,
                watchHistoryItem = watchHistoryItem.value,
                episode = currentSelectedEpisode.value,
                onSuccess = { _ -> 
                    resetUiState()
                    resetNextEpisodeQueue()
                },
                runWebView = runWebView,
                onError = {
                    updateProviderSelected(oldSelectedSource)
                    showErrorOnUiCallback(UiText.StringResource(UtilR.string.failed_to_retrieve_provider_message_format))
                }
            ).collect { state ->
                when (state) {
                    SourceDataState.Idle,
                    is SourceDataState.Error,
                    is SourceDataState.Unavailable,
                    SourceDataState.Success -> _uiState.update {
                        it.copy(selectedProviderState = Resource.Success(null))
                    }

                    is SourceDataState.Extracting, is SourceDataState.Fetching -> _uiState.update {
                        it.copy(selectedProviderState = Resource.Loading)
                    }
                }
            }
        }
    }

    fun onResizeModeChange(resizeMode: Int) {
        _uiState.update { it.copy(selectedResizeMode = resizeMode) }
    }

    fun onPanelChange(opened: Int) {
        _uiState.update { it.copy(lastOpenedPanel = opened) }
    }

    fun onConsumePlayerDialog() {
        _dialogState.update { SourceDataState.Idle }
    }

    fun updateVolume(strength: Float) {
        _uiState.update {
            it.copy(volume = strength)
        }
    }

    fun updateScreenBrightness(strength: Float) {
        _uiState.update {
            it.copy(screenBrightness = strength)
        }
    }

    fun onServerChange(index: Int? = null) {
        val indexToUse = index ?: sourceData.cachedLinks.indexOfFirst {
            it.name.contains(appSettings.value.preferredQuality.qualityName, true)
            || it.name.equals(appSettings.value.preferredQuality.qualityName, true)
        }

        _uiState.update {
            it.copy(selectedSourceLink = max(indexToUse, 0))
        }
    }

    fun toggleVideoTimeReverse() {
        viewModelScope.launch {
            appSettingsManager.updateSettings(
                appSettings.value.run {
                    copy(isPlayerTimeReversed = !isPlayerTimeReversed)
                }
            )
        }
    }

    private fun updateProviderSelected(source: String?) {
        _uiState.update {
            it.copy(selectedProvider = source)
        }
    }

    fun updateWatchHistory(
        currentTime: Long,
        duration: Long,
    ) {
        viewModelScope.launch {
            watchTimeUpdaterUseCase(
                watchHistoryItem = watchHistoryItem.value ?: film.toWatchHistoryItem(),
                currentTime = currentTime,
                totalDuration = duration,
                currentSelectedEpisode = currentSelectedEpisode.value
            )
        }
    }

    /**
     *
     * Obtains the queried [Season] data with the given parameter [seasonNumber]
     * from local state or remote fetch.
     *
     * See: [fetchSeasonFromMetaProvider]
     *
     * @param seasonNumber the season to be queried.
     *
     * */
    private suspend fun fetchSeasonIfNeeded(seasonNumber: Int): Resource<Season?> {
        var currentLoadedSeasonNumber = _season.value
        if (currentLoadedSeasonNumber.data?.seasonNumber != seasonNumber) {
            _season.update {
                fetchSeasonFromMetaProvider(
                    seasonNumber = seasonNumber
                )
            }

            currentLoadedSeasonNumber = _season.value
        }

        return currentLoadedSeasonNumber
    }

    /**
     *
     * Obtains the next episode based on the given [TMDBEpisode].
     * It returns null if an error has occured or it can't find the episode to be queried.
     *
     * @param onError an optional callback if caller wants to call [showErrorOnUiCallback]
     *
     * */
    private suspend fun TMDBEpisode.getNextEpisode(
        onError: ((UiText) -> Unit)? = null,
    ): TMDBEpisode? {
        val nextEpisode: TMDBEpisode?

        val episodeSeasonNumber = this.season
        val episodeNumber = this.episode

        val seasonToUse = fetchSeasonIfNeeded(episodeSeasonNumber)

        if(seasonToUse is Resource.Failure) {
            onError?.invoke(seasonToUse.error!!)
            return null
        }

        val episodesList = seasonToUse.data!!.episodes
        val nextEpisodeNumberToWatch = episodeNumber + 1

        if (episodesList.last().episode == nextEpisodeNumberToWatch)
            return episodesList.last()

        val isThereANextEpisode = episodesList.last().episode > nextEpisodeNumberToWatch
        val isThereANextSeason = seasonToUse.data!!.seasonNumber + 1 <= (film as TvShow).totalSeasons

        nextEpisode = if (isThereANextEpisode) {
            episodesList.firstOrNull {
                it.episode == nextEpisodeNumberToWatch
            }
        } else if (isThereANextSeason) {
            fetchSeasonIfNeeded(seasonNumber = episodeSeasonNumber + 1).run {
                if(this is Resource.Failure) {
                    onError?.invoke(error!!)
                }

                data?.episodes?.firstOrNull()
            }
        } else null

        if(nextEpisode == null && !isThereANextSeason) {
            onError?.invoke(
                UiText.StringResource(
                    UtilR.string.episode_non_existent_error_message_format,
                    nextEpisodeNumberToWatch,
                    seasonToUse.data!!.seasonNumber
                )
            )
        }

        return nextEpisode
    }

    /**
     * Callback function triggered when an episode is clicked.
     *
     * @param episodeToWatch The next episode to be played, or null if not available.
     */
    fun onEpisodeClick(
        episodeToWatch: TMDBEpisode? = null,
        runWebView: (FlixclusiveWebView) -> Unit
    ) {
        if (loadLinksFromNewProviderJob?.isActive == true || loadLinksJob?.isActive == true) {
            showErrorOnUiCallback(UiText.StringResource(UtilR.string.load_link_job_active_error_message))
            return
        }

        // Cancel next episode queueing job
        if (loadNextLinksJob?.isActive == true) {
            loadNextLinksJob?.cancel()
            loadNextLinksJob = null
        }

        viewModelScope.launch {
            // If there's a queued next episode, stop the video and go next
            val isLoadingNextEpisode = episodeToWatch == null
            val hasNextEpisode = getQueuedEpisode()

            if (isLoadingNextEpisode && hasNextEpisode) {
                return@launch
            }

            var episode = episodeToWatch
            if (isLoadingNextEpisode) {
                episode = currentSelectedEpisode.value!!.getNextEpisode(
                    onError = { showErrorOnUiCallback(it) }
                ).also {
                    if (it == null) {
                        return@launch
                    }
                }
            }

            loadSourceData(episode, runWebView)
        }
    }

    /**
     * Function to load [SourceData] from a provider
     *
     * @param episodeToWatch an optional parameter for the episode to watch if film to be watched is a [TvShow]
     */
    fun loadSourceData(
        episodeToWatch: TMDBEpisode? = null,
        runWebView: (FlixclusiveWebView) -> Unit
    ) {
        if (loadLinksFromNewProviderJob?.isActive == true || loadLinksJob?.isActive == true) {
            showErrorOnUiCallback(UiText.StringResource(UtilR.string.load_link_job_active_error_message))
            return
        }

        // Cancel next episode queueing job
        if (loadNextLinksJob?.isActive == true) {
            loadNextLinksJob?.cancel()
            loadNextLinksJob = null
        }

        loadLinksJob = viewModelScope.launch {
            sourceLinksProvider.loadLinks(
                film = film,
                mediaId = sourceData.mediaId,
                preferredProviderName = _uiState.value.selectedProvider,
                watchHistoryItem = watchHistoryItem.value,
                episode = episodeToWatch,
                runWebView = runWebView,
                onSuccess = { newEpisode ->
                    _currentSelectedEpisode.value = newEpisode

                    resetUiState()
                    resetNextEpisodeQueue()
                }
            ).collect {
                _dialogState.value = it
            }
        }
    }

    /**
     *
     * Obtains the next episode queued by the player.
     *
     * @return [Boolean] true if success, otherwise false.
     * */
    private suspend fun getQueuedEpisode(): Boolean {
        if (
            isNextEpisodeLoaded
            && nextEpisodeToUse != null
        ) {
            _season.value = fetchSeasonFromMetaProvider(nextEpisodeToUse!!.season)
            _currentSelectedEpisode.value = nextEpisodeToUse

            resetUiState()
            resetNextEpisodeQueue()

            return true
        }

        return false
    }

    /**
     * Reset the episode queue
     * */
    private fun resetNextEpisodeQueue() {
        nextEpisodeToUse = null
        isNextEpisodeLoaded = false
    }

    /**
     *
     * Callback function to silently queue up the next episode
     */
    fun onQueueNextEpisode(
        runWebView: (FlixclusiveWebView) -> Unit
    ) {
        if (loadNextLinksJob?.isActive == true || isNextEpisodeLoaded || loadLinksJob?.isActive == true || loadLinksFromNewProviderJob?.isActive == true)
            return

        loadNextLinksJob = viewModelScope.launch {
            val episode = currentSelectedEpisode.value!!
                .getNextEpisode()

            sourceLinksProvider.loadLinks(
                film = film,
                mediaId = sourceData.mediaId,
                preferredProviderName = _uiState.value.selectedProvider,
                watchHistoryItem = watchHistoryItem.value,
                episode = episode,
                runWebView = runWebView,
                onSuccess = { newEpisode ->
                    nextEpisodeToUse = newEpisode
                    isNextEpisodeLoaded = true
                }
            ).collect()
        }
    }

    /**
     *
     * Fetches the [Season] data from the main meta provider
     * then if it successfully obtains the data, save it to
     * the watch history episodes map for local use.
     *
     * @param seasonNumber the season to be queried.
     *
     * */
    protected suspend fun fetchSeasonFromMetaProvider(seasonNumber: Int)
        = seasonProviderUseCase(id = film.id, seasonNumber = seasonNumber)
}