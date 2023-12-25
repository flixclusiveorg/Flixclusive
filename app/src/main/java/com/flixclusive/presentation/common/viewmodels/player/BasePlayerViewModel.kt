package com.flixclusive.presentation.common.viewmodels.player

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.domain.common.Resource
import com.flixclusive.domain.model.VideoDataDialogState
import com.flixclusive.domain.model.entities.WatchHistoryItem
import com.flixclusive.domain.model.tmdb.Film
import com.flixclusive.domain.model.tmdb.FilmType
import com.flixclusive.domain.model.tmdb.Season
import com.flixclusive.domain.model.tmdb.TMDBEpisode
import com.flixclusive.domain.preferences.AppSettingsManager
import com.flixclusive.domain.repository.ProvidersRepository
import com.flixclusive.domain.usecase.VideoDataProviderUseCase
import com.flixclusive.domain.utils.WatchHistoryUtils
import com.flixclusive.presentation.common.player.PlayerUiState
import com.flixclusive.providers.models.common.VideoData
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
import kotlin.math.max

@Suppress("PropertyName")
abstract class BasePlayerViewModel(
    providersRepository: ProvidersRepository,
    private val appSettingsManager: AppSettingsManager,
    private val videoDataProvider: VideoDataProviderUseCase,
) : ViewModel() {
    val sourceProviders = providersRepository.providers
        .filter { !it.isIgnored && !it.isMaintenance }
        .map { it.source.name }

    private val _dialogState = MutableStateFlow<VideoDataDialogState>(VideoDataDialogState.Idle)
    val dialogState: StateFlow<VideoDataDialogState> = _dialogState.asStateFlow()

    var areControlsVisible by mutableStateOf(true)
    var areControlsLocked by mutableStateOf(false)
    var isLastEpisode by mutableStateOf(false)
        private set
    private var isNextEpisodeLoaded = false
    protected val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    protected val _videoData = MutableStateFlow(VideoData())
    open val videoData = _videoData.asStateFlow()

    /**
     * For the next episode to
     * seamlessly watch tv shows
     * */
    private var nextEpisodeToUse: TMDBEpisode? by mutableStateOf(null)
    private var nextVideoData: VideoData? by mutableStateOf(null)

    abstract val watchHistoryItem: StateFlow<WatchHistoryItem?>

    protected val _currentSelectedEpisode = MutableStateFlow<TMDBEpisode?>(null)
    open val currentSelectedEpisode = _currentSelectedEpisode.asStateFlow()

    val appSettings = appSettingsManager.appSettings
        .data
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = appSettingsManager.localAppSettings
        )

    private var onLoadVideoSource: Job? = null

    open fun resetUiState(data: VideoData? = null) {
        val dataToUse = data ?: videoData.value

        _uiState.update {
            val preferredServer = appSettings.value.preferredQuality
            val indexOfPreferredServer = dataToUse.servers?.indexOfFirst { server ->
                server.serverName.contains(preferredServer, true)
                        || server.serverName == preferredServer
            } ?: 0

            PlayerUiState(
                screenBrightness = it.screenBrightness,
                selectedServer = max(indexOfPreferredServer, 0),
                volume = it.volume,
                lastOpenedPanel = it.lastOpenedPanel,
                selectedResizeMode = it.selectedResizeMode,
                selectedSource = dataToUse.sourceName,
            )
        }
    }

    // =======================
    fun getSavedTimeForVideoData(episode: TMDBEpisode? = null): Pair<Long, Long> {
        val episodeToUse = episode ?: currentSelectedEpisode.value

        val watchHistoryItemToUse = watchHistoryItem.value ?: WatchHistoryItem()

        isLastEpisode = checkIfLastEpisode(episodeToUse, watchHistoryItemToUse)
        val currentTime = WatchHistoryUtils.getLastWatchTime(
            watchHistoryItemToUse,
            episodeToUse
        )
        val totalDuration = WatchHistoryUtils.getTotalDuration(
            watchHistoryItemToUse,
            episodeToUse
        )

        return currentTime to totalDuration
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

    protected abstract fun onSuccessCallback(newData: VideoData, newEpisode: TMDBEpisode?)

    protected abstract fun onErrorCallback(message: String? = null)

    fun onServerChange(serverIndex: Int) {
        videoData.value.run {
            updateServerSelected(index = serverIndex)
            val server = servers!![serverIndex]
            onSuccessCallback(
                newData = copy(source = server.serverUrl),
                newEpisode = currentSelectedEpisode.value
            )
        }
    }

    protected fun onSourceChange(
        film: Film,
        newSource: String,
    ) {
        if (onLoadVideoSource?.isActive == true)
            return

        onLoadVideoSource = viewModelScope.launch {
            val oldSelectedSource = _uiState.value.selectedSource
            updateSourceSelected(newSource)

            videoDataProvider(
                film = film,
                source = newSource,
                server = appSettings.value.preferredQuality,
                watchHistoryItem = watchHistoryItem.value,
                episode = currentSelectedEpisode.value,
                onSuccess = { newData, newEpisode ->
                    onSuccessCallback(
                        newData = newData,
                        newEpisode = newEpisode
                    )
                },
                onError = {
                    updateSourceSelected(oldSelectedSource)
                    onErrorCallback("Failed to retrieve source [$newSource]")
                }
            ).collect { state ->
                when (state) {
                    VideoDataDialogState.Idle, is VideoDataDialogState.Error, is VideoDataDialogState.Unavailable, VideoDataDialogState.Success -> _uiState.update {
                        it.copy(selectedSourceState = Resource.Success(null))
                    }

                    is VideoDataDialogState.Extracting, is VideoDataDialogState.Fetching -> _uiState.update {
                        it.copy(selectedSourceState = Resource.Loading)
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
        _dialogState.update { VideoDataDialogState.Idle }
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

    private fun updateServerSelected(index: Int? = null) {
        val indexToUse = index
            ?: videoData.value.servers?.indexOfFirst {
                it.serverName.contains(appSettings.value.preferredQuality, true)
                        || it.serverName == appSettings.value.preferredQuality
            } ?: 0

        _uiState.update {
            it.copy(selectedServer = if (indexToUse == -1) 0 else indexToUse)
        }
    }

    fun toggleVideoTimeReverse() {
        viewModelScope.launch {
            appSettingsManager.updateData(
                appSettings.value.run {
                    copy(isPlayerTimeReversed = !isPlayerTimeReversed)
                }
            )
        }
    }

    private fun updateSourceSelected(source: String?) {
        _uiState.update {
            it.copy(selectedSource = source)
        }
    }

    abstract fun updateWatchHistory(
        currentTime: Long,
        duration: Long,
    )

    protected abstract suspend fun TMDBEpisode.fetchSeasonIfNeeded(seasonNumber: Int): Season?

    private suspend fun TMDBEpisode.getNextEpisode(
        seasonCount: Int,
        updateSeason: ((Season?) -> Unit)? = null,
    ): TMDBEpisode {
        val nextEpisode: TMDBEpisode

        val episodeSeasonNumber = this.season
        val episodeNumber = this.episode

        var seasonToUse = fetchSeasonIfNeeded(episodeSeasonNumber)

        val episodesList = seasonToUse!!.episodes
        val nextEpisodeNumberToWatch = episodeNumber + 1

        if (episodesList.last().episode == nextEpisodeNumberToWatch)
            return episodesList.last()

        nextEpisode = if (episodesList.last().episode > nextEpisodeNumberToWatch) {
            episodesList.find {
                it.episode == nextEpisodeNumberToWatch
            }
                ?: throw NullPointerException("Episode $nextEpisodeNumberToWatch doesn't exist on Season ${seasonToUse.seasonNumber}")
        } else if (seasonToUse.seasonNumber + 1 <= seasonCount) {
            seasonToUse =
                fetchSeasonIfNeeded(seasonNumber = currentSelectedEpisode.value!!.season + 1)

            updateSeason?.invoke(seasonToUse)

            seasonToUse!!.episodes.first()
        } else throw NullPointerException("Episode $nextEpisodeNumberToWatch doesn't exist on Season ${seasonToUse.seasonNumber}")

        return nextEpisode
    }

    /**
     * Function to load video source data from providers
     *
     */
    fun loadVideoData(
        film: Film,
        seasonCount: Int?,
        episodeToWatch: TMDBEpisode? = null,
        updateSeason: ((Season?) -> Unit)? = null,
    ) {
        if (onLoadVideoSource?.isActive == true)
            return

        onLoadVideoSource = viewModelScope.launch {
            // If there's a queued next episode, stop the video and go next
            val isLoadingNextEpisode = episodeToWatch == null && film.filmType == FilmType.TV_SHOW
            if (
                isLoadingNextEpisode &&
                getQueuedEpisode(
                    seasonCount!!,
                    ::onSuccessCallback,
                    updateSeason
                )
            ) {
                return@launch
            }

            var episode = episodeToWatch
            if (episode == null) {
                episode = currentSelectedEpisode.value!!.getNextEpisode(
                    seasonCount = seasonCount!!,
                    updateSeason = updateSeason
                )
            }

            val server = videoData.value.servers?.get(uiState.value.selectedServer)?.serverName
            videoDataProvider(
                film = film,
                mediaId = videoData.value.mediaId,
                server = server,
                source = _uiState.value.selectedSource,
                watchHistoryItem = watchHistoryItem.value,
                episode = episode,
                onSuccess = { newData, newEpisode ->
                    onSuccessCallback(newData, newEpisode)
                    resetUiState(newData)

                    resetNextEpisodeQueue()
                }
            ).collectLatest { state ->
                _dialogState.update { state }
            }
        }
    }

    private suspend fun getQueuedEpisode(
        seasonCount: Int,
        onSuccess: (VideoData, TMDBEpisode?) -> Unit,
        updateSeason: ((Season?) -> Unit)? = null,
    ): Boolean {
        if (
            isNextEpisodeLoaded
            && nextVideoData != null
            && nextEpisodeToUse != null
        ) {
            currentSelectedEpisode.value!!.getNextEpisode(
                seasonCount = seasonCount,
                updateSeason = updateSeason
            )
            onSuccess(nextVideoData!!, nextEpisodeToUse)
            resetUiState(nextVideoData)

            resetNextEpisodeQueue()

            return true
        }

        return false
    }

    // Reset the episode queue
    private fun resetNextEpisodeQueue() {
        nextVideoData = null
        nextEpisodeToUse = null
        isNextEpisodeLoaded = false
    }

    open fun queueNextEpisode(
        film: Film,
        seasonCount: Int,
    ) {
        if (onLoadVideoSource?.isActive == true || isNextEpisodeLoaded)
            return

        onLoadVideoSource = viewModelScope.launch {
            val episode = currentSelectedEpisode.value!!
                .getNextEpisode(seasonCount = seasonCount)

            val server = videoData.value.servers?.get(uiState.value.selectedServer)?.serverName
            videoDataProvider(
                film = film,
                mediaId = videoData.value.mediaId,
                server = server,
                source = _uiState.value.selectedSource,
                watchHistoryItem = watchHistoryItem.value,
                episode = episode,
                onSuccess = { newData, newEpisode ->
                    nextVideoData = newData
                    nextEpisodeToUse = newEpisode
                    isNextEpisodeLoaded = true
                }
            ).collect()
        }
    }

    protected abstract suspend fun fetchSeasonFromProvider(showId: Int, seasonNumber: Int): Season?
}