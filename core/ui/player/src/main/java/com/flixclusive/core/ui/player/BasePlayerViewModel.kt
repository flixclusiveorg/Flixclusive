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
import com.flixclusive.core.util.film.FilmType
import com.flixclusive.data.watch_history.WatchHistoryRepository
import com.flixclusive.domain.database.WatchTimeUpdaterUseCase
import com.flixclusive.domain.provider.SourceLinksProviderUseCase
import com.flixclusive.domain.tmdb.SeasonProviderUseCase
import com.flixclusive.model.database.WatchHistoryItem
import com.flixclusive.model.database.toWatchHistoryItem
import com.flixclusive.model.database.util.getSavedTimeForFilm
import com.flixclusive.model.provider.SourceData
import com.flixclusive.model.provider.SourceDataState
import com.flixclusive.model.tmdb.FilmDetails
import com.flixclusive.model.tmdb.TvShow
import com.flixclusive.model.tmdb.common.tv.Episode
import com.flixclusive.model.tmdb.common.tv.Season
import com.flixclusive.provider.util.FlixclusiveWebView
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.onCompletion
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

    var webView: FlixclusiveWebView? by mutableStateOf(null)
        private set
    val player = FlixclusivePlayerManager(
        client = client,
        context = context,
        playerCacheManager = playerCacheManager,
        appSettings = appSettingsManager.localAppSettings,
        showErrorCallback = {
            showErrorSnackbar(
                message = it,
                isInternalPlayerError = true
            )
        }
    )

    val sourceData: SourceData
        get() = sourceLinksProvider.getLinks(
            filmId = film.identifier,
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
    private var nextEpisodeToUse: Episode? by mutableStateOf(null)
    private var isNextEpisodeLoaded = false
    var isLastEpisode by mutableStateOf(film.filmType == FilmType.MOVIE)
        private set

    val watchHistoryItem = watchHistoryRepository
        .getWatchHistoryItemByIdInFlow(film.identifier)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = runBlocking { watchHistoryRepository.getWatchHistoryItemById(film.identifier) ?: film.toWatchHistoryItem() }
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
            val preferredResizeMode = appSettings.value.preferredResizeMode

            val indexOfPreferredServer = sourceData.cachedLinks.indexOfFirst { server ->
                server.name.contains(preferredServer.qualityName, true)
                || server.name.equals(preferredServer.qualityName, true)
            }

            it.copy(
                selectedSourceLink = max(indexOfPreferredServer, 0),
                selectedResizeMode = preferredResizeMode,
                selectedProvider = sourceData.providerName
            )
        }
    }

    fun onRunWebView(webView: FlixclusiveWebView) {
        this.webView = webView
    }

    fun onDestroyWebView() {
        webView?.destroy()
        webView = null
    }

    /**
     *
     * Obtains the saved time for the current [SourceData] to be watched.
     *
     * @param episode an optional parameter for episode to be watched if the film is a [TvShow]
     *
     * @return A pair of time in [Long] format - Pair(watchTime, durationTime)
     * */
    fun getSavedTimeForSourceData(episode: Episode? = null): Pair<Long, Long> {
        val episodeToUse = episode ?: currentSelectedEpisode.value

        val watchHistoryItemToUse = watchHistoryItem.value ?: return 0L to 0L

        isLastEpisode = checkIfLastEpisode(
            episodeToCheck = episodeToUse,
            watchHistoryItem = watchHistoryItemToUse
        )

        return getSavedTimeForFilm(
            watchHistoryItemToUse,
            episodeToUse
        )
    }

    private fun checkIfLastEpisode(
        episodeToCheck: Episode?,
        watchHistoryItem: WatchHistoryItem?,
    ): Boolean {
        val episode = episodeToCheck ?: return true
        val lastSeason = watchHistoryItem?.seasons
        val lastEpisode = watchHistoryItem?.episodes?.get(lastSeason)

        return episode.season == lastSeason && episode.number == lastEpisode
    }

    /**
     *
     * A callback to show an error message to the player screen.
     *
     * @param message the global [UiText] message to be sent and parse by the player
     * */
    protected abstract fun showErrorSnackbar(
        message: UiText,
        isInternalPlayerError: Boolean = false
    )

    fun onSeasonChange(seasonNumber: Int) {
        if (onSeasonChangeJob?.isActive == true)
            return

        onSeasonChangeJob = viewModelScope.launch {
            if (!film.isFromTmdb) {
                val tvShow = film as TvShow
                val season = tvShow.seasons
                    .find { it.number == seasonNumber }

                if (season != null)
                    _season.value = Resource.Success(season)

                return@launch
            }

            seasonProviderUseCase.asFlow(
                tvShow = film as TvShow,
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
                film = film as FilmDetails,
                preferredProviderName = newProvider,
                watchHistoryItem = watchHistoryItem.value,
                episode = currentSelectedEpisode.value,
                onSuccess = { _ ->
                    onDestroyWebView()

                    resetUiState()
                    resetNextEpisodeQueue()
                },
                runWebView = runWebView,
                onError = {
                    updateProviderSelected(oldSelectedSource)
                    showErrorSnackbar(it)
                }
            ).onCompletion {
                if (it != null) {
                    onDestroyWebView()
                }
            }.collect { state ->
                when (state) {
                    SourceDataState.Idle,
                    is SourceDataState.Error,
                    is SourceDataState.Unavailable,
                    SourceDataState.Success -> _uiState.update {
                        it.copy(selectedProviderState = PlayerProviderState.SELECTED)
                    }

                    is SourceDataState.Extracting, is SourceDataState.Fetching -> _uiState.update {
                        it.copy(selectedProviderState = PlayerProviderState.LOADING)
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
        if (currentLoadedSeasonNumber.data?.number != seasonNumber) {
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
     * Obtains the next episode based on the given [Episode].
     * It returns null if an error has occured or it can't find the episode to be queried.
     *
     * @param onError an optional callback if caller wants to call [showErrorSnackbar]
     *
     * */
    private suspend fun Episode.getNextEpisode(
        onError: ((UiText) -> Unit)? = null,
    ): Episode? {
        val nextEpisode: Episode?

        val episodeSeasonNumber = this.season
        val episodeNumber = this.number

        val seasonToUse = fetchSeasonIfNeeded(episodeSeasonNumber)

        if(seasonToUse is Resource.Failure) {
            onError?.invoke(seasonToUse.error!!)
            return null
        }

        val episodesList = seasonToUse.data!!.episodes
        val nextEpisodeNumberToWatch = episodeNumber + 1

        if (episodesList.last().number == nextEpisodeNumberToWatch)
            return episodesList.last()

        val isThereANextEpisode = episodesList.last().number > nextEpisodeNumberToWatch
        val isThereANextSeason = seasonToUse.data!!.number + 1 <= (film as TvShow).totalSeasons

        nextEpisode = if (isThereANextEpisode) {
            episodesList.firstOrNull {
                it.number == nextEpisodeNumberToWatch
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
                    seasonToUse.data!!.number
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
        episodeToWatch: Episode? = null,
        runWebView: (FlixclusiveWebView) -> Unit
    ) {
        if (loadLinksFromNewProviderJob?.isActive == true || loadLinksJob?.isActive == true) {
            showErrorSnackbar(UiText.StringResource(UtilR.string.load_link_job_active_error_message))
            return
        }

        // Cancel next episode queueing job
        if (loadNextLinksJob?.isActive == true) {
            loadNextLinksJob?.cancel()
            loadNextLinksJob = null
        }

        viewModelScope.launch {
            // If there's a queued next episode, skip loading the video and go next
            val isLoadingNextEpisode = episodeToWatch == null
            val hasNextEpisode = getQueuedEpisode()

            if (isLoadingNextEpisode && hasNextEpisode) {
                return@launch
            }

            var episode = episodeToWatch
            if (isLoadingNextEpisode) {
                episode = currentSelectedEpisode.value!!.getNextEpisode(
                    onError = { showErrorSnackbar(it) }
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
        episodeToWatch: Episode? = null,
        runWebView: (FlixclusiveWebView) -> Unit
    ) {
        if (loadLinksFromNewProviderJob?.isActive == true || loadLinksJob?.isActive == true) {
            showErrorSnackbar(UiText.StringResource(UtilR.string.load_link_job_active_error_message))
            return
        }

        // Cancel next episode queueing job
        if (loadNextLinksJob?.isActive == true) {
            loadNextLinksJob?.cancel()
            loadNextLinksJob = null
        }

        loadLinksJob = viewModelScope.launch {
            sourceLinksProvider.loadLinks(
                film = film as FilmDetails,
                watchId = sourceData.watchId,
                preferredProviderName = _uiState.value.selectedProvider,
                watchHistoryItem = watchHistoryItem.value,
                episode = episodeToWatch,
                runWebView = runWebView,
                onSuccess = { newEpisode ->
                    onDestroyWebView()

                    _currentSelectedEpisode.value = newEpisode

                    resetUiState()
                    resetNextEpisodeQueue()
                }
            ).onCompletion {
                if (it != null) {
                    onDestroyWebView()
                }
            }.collect {
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
                film = film as FilmDetails,
                watchId = sourceData.watchId,
                preferredProviderName = _uiState.value.selectedProvider,
                watchHistoryItem = watchHistoryItem.value,
                episode = episode,
                runWebView = runWebView,
                onSuccess = { newEpisode ->
                    onDestroyWebView()

                    nextEpisodeToUse = newEpisode
                    isNextEpisodeLoaded = true
                }
            ).onCompletion {
                if (it != null) {
                    onDestroyWebView()
                }
            }.collect()
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
        = seasonProviderUseCase(
            tvShow = film as TvShow,
            seasonNumber = seasonNumber
        )
}