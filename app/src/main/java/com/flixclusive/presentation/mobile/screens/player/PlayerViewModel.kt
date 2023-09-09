package com.flixclusive.presentation.mobile.screens.player

import androidx.compose.runtime.mutableStateListOf
import androidx.core.net.toUri
import androidx.datastore.core.DataStore
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem.SubtitleConfiguration
import androidx.media3.common.MimeTypes.TEXT_VTT
import androidx.media3.common.Player
import androidx.media3.common.TrackGroup
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import com.flixclusive.di.DefaultDispatcher
import com.flixclusive.domain.common.Resource
import com.flixclusive.domain.model.entities.WatchHistoryItem
import com.flixclusive.domain.model.tmdb.Season
import com.flixclusive.domain.model.tmdb.TMDBEpisode
import com.flixclusive.domain.preferences.AppSettings
import com.flixclusive.domain.usecase.SeasonProviderUseCase
import com.flixclusive.domain.usecase.VideoDataProviderUseCase
import com.flixclusive.domain.usecase.WatchHistoryItemManagerUseCase
import com.flixclusive.domain.utils.FilmProviderUtils.getDefaultSubtitleIndex
import com.flixclusive.domain.utils.WatchHistoryUtils.getLastWatchTime
import com.flixclusive.presentation.common.PlayerUiState
import com.flixclusive.domain.model.VideoDataDialogState
import com.flixclusive.domain.utils.WatchHistoryUtils.getTotalDuration
import com.flixclusive_provider.models.common.VideoData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import javax.inject.Inject

@UnstableApi
@HiltViewModel
class PlayerViewModel @Inject constructor(
    @DefaultDispatcher val defaultDispatcher: CoroutineDispatcher,
    private val videoDataProvider: VideoDataProviderUseCase,
    private val _appSettings: DataStore<AppSettings>,
    private val seasonProvider: SeasonProviderUseCase,
    private val watchHistoryItemManager: WatchHistoryItemManagerUseCase,
    val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val snackbarQueue = mutableStateListOf<PlayerSnackbarMessage>()

    private val _dialogState = MutableStateFlow<VideoDataDialogState>(VideoDataDialogState.Idle)
    val dialogState: StateFlow<VideoDataDialogState> = _dialogState.asStateFlow()

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    val videoData = savedStateHandle.getStateFlow(VIDEO_DATA, VideoData())
    var watchHistoryItem = savedStateHandle.getStateFlow(WATCH_HISTORY_ITEM, WatchHistoryItem())

    // Only valid if video data is a tv show
    val seasonCount = savedStateHandle.getStateFlow<Int?>(SEASON_COUNT, null)
    val currentSelectedEpisode = savedStateHandle.getStateFlow<TMDBEpisode?>(EPISODE_SELECTED, null)
    private val _season = MutableStateFlow<Resource<Season>?>(null)
    val season = _season.asStateFlow()

    val appSettings = _appSettings.data
        .drop(1)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = runBlocking { _appSettings.data.first() }
        )

    private var videoTrackGroup: TrackGroup? = null
    private val qualityTrackGroups = mutableListOf<Int>()
    val availableQualities = mutableStateListOf<String>()
    val availableSubtitles = mutableStateListOf<SubtitleConfiguration>()

    private var onSeasonChangeJob: Job? = null
    private var onShowSnackbarJob: Job? = null
    private var onRemoveSnackbarJob: Job? = null
    private var onLoadVideoSource: Job? = null

    init {
        initialize()
    }

    fun initialize() {
        videoTrackGroup = null
        qualityTrackGroups.clear()
        availableQualities.clear()
        availableSubtitles.clear()

        _uiState.update {
            PlayerUiState(
                currentTime = getLastWatchTime(
                    watchHistoryItem.value,
                    currentSelectedEpisode.value
                ),
                totalDuration = getTotalDuration(
                    watchHistoryItem.value,
                    currentSelectedEpisode.value
                ),
            )
        }

        viewModelScope.launch {
            if (currentSelectedEpisode.value != null) {
                if(currentSelectedEpisode.value!!.season != seasonCount.value) {
                    fetchSeasonFromProvider(seasonCount.value!!)
                }

                onSeasonChange(currentSelectedEpisode.value!!.season)
            }
        }

        startExtractingSubtitles()
        val subtitleToUse = when {
            !appSettings.value.isSubtitleEnabled -> 0
            else -> videoData.value.getDefaultSubtitleIndex()
        }

        updateServerSelected()
        updateSubtitle(subtitleToUse)
    }

    fun updatePlayerState(
        isPlaying: Boolean,
        totalDuration: Long = 0L,
        currentTime: Long = 0L,
        bufferedPercentage: Int = 0,
        playbackState: Int,
    ) {
        _uiState.update {
            it.copy(
                totalDuration = totalDuration,
                currentTime = currentTime,
                bufferedPercentage = bufferedPercentage,
                isPlaying = isPlaying,
                playbackState = playbackState
            )
        }
    }

    fun onActivityStop(
        playWhenReady: Boolean,
        currentTime: Long,
    ) {
        _uiState.update {
            it.copy(
                playWhenReady = playWhenReady,
                currentTime = currentTime,
                playbackState = Player.STATE_IDLE,
                isPlaying = false,
            )
        }
    }

    /**
     * Observes the latest video data and
     * extracts the subtitle configurations of it.
     *
     */
    private fun startExtractingSubtitles() {
        viewModelScope.launch {
            videoData.collectLatest {
                availableSubtitles.clear()
                it.subtitles.forEach { subtitle ->
                    val mimeType = when (subtitle.url.split(".").last()) {
                        "vtt" -> TEXT_VTT
                        else -> null
                    }

                    val subtitleConfiguration = SubtitleConfiguration
                        .Builder(subtitle.url.toUri())
                        .setMimeType(mimeType)
                        .setLanguage(subtitle.lang.lowercase())
                        .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                        .build()

                    availableSubtitles.add(subtitleConfiguration)
                }
            }
        }
    }

    /**
     * Initializes the video qualities by populating the available qualities and track groups.
     */
    fun initializeVideoQualities(tracks: Tracks) {
        if (qualityTrackGroups.isEmpty()) {
            val videoTrackGroups = tracks.groups
                .filter { group ->
                    group.type == C.TRACK_TYPE_VIDEO
                }

            videoTrackGroups.forEach { group ->
                val groupInfo = group.mediaTrackGroup
                videoTrackGroup = groupInfo
                for (trackIndex in 0 until groupInfo.length) {
                    qualityTrackGroups.add(trackIndex)
                    availableQualities.add("${groupInfo.getFormat(trackIndex).height}p")
                }
            }

            if(availableQualities.size > 1) {
                availableQualities.add(
                    index = 0,
                    element = "Auto"
                )
            }
        }
    }

    /**
     * Callback function triggered when the subtitles are changed.
     *
     * @param subtitleIndex The index of the selected subtitle.
     */
    fun onSubtitleChange(
        subtitleIndex: Int,
        trackParameters: TrackSelectionParameters,
    ): TrackSelectionParameters {
        updateSubtitle(subtitleIndex)

        return when (subtitleIndex) {
            0 -> {
                trackParameters
                    .buildUpon()
                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                    .build()
            }
            else -> {
                trackParameters
                    .buildUpon()
                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                    .setPreferredTextLanguage(videoData.value.subtitles[subtitleIndex].lang.lowercase())
                    .build()
            }
        }
    }

    /**
     * Callback function triggered when the video quality is changed.
     *
     * @param qualityIndex The index of the selected video quality.
     */
    fun onVideoQualityChange(
        qualityIndex: Int,
        trackParameters: TrackSelectionParameters,
    ): TrackSelectionParameters? {
        updateVideoQuality(qualityIndex)

        return videoTrackGroup?.let { group ->
            when (qualityIndex) {
                0 -> {
                    trackParameters
                        .buildUpon()
                        .clearOverridesOfType(C.TRACK_TYPE_VIDEO)
                        .build()
                }
                else -> {
                    trackParameters
                        .buildUpon()
                        .setOverrideForType(
                            TrackSelectionOverride(group, qualityTrackGroups[qualityIndex - 1])
                        )
                        .build()
                }
            }
        }
    }

    fun onVideoServerChange(serverIndex: Int) {
        if(onLoadVideoSource?.isActive == true)
            return

        onLoadVideoSource = viewModelScope.launch {
            updateServerSelected(serverIndex)
            val selectedServer = videoData.value.servers!![serverIndex].serverName

            videoDataProvider(
                film = watchHistoryItem.value.film,
                mediaId = videoData.value.mediaId,
                server = selectedServer,
                watchHistoryItem = watchHistoryItem.value,
                episode = currentSelectedEpisode.value,
                onSuccess = { newData, _ ->
                    videoTrackGroup = null
                    qualityTrackGroups.clear()
                    availableQualities.clear()
                    availableSubtitles.clear()

                    _uiState.update {
                        it.copy(selectedQuality = 0)
                    }

                    startExtractingSubtitles()
                    val subtitleToUse = when {
                        !appSettings.value.isSubtitleEnabled -> 0
                        else -> newData.getDefaultSubtitleIndex()
                    }
                    updateSubtitle(subtitleToUse)

                    savedStateHandle[VIDEO_DATA] = newData
                }
            ).collect()
        }
    }

    fun onPlaybackSpeedChange(speedIndex: Int) {
        _uiState.update { it.copy(selectedPlaybackSpeedIndex = speedIndex) }
    }

    /**
     * Callback function triggered when an episode is clicked.
     *
     * @param episodeToWatch The next episode to be played, or null if not available.
     */
    fun onEpisodeClick(episodeToWatch: TMDBEpisode? = null) {
        if(onLoadVideoSource?.isActive == true)
            return

        onLoadVideoSource = viewModelScope.launch {
            var episode = episodeToWatch
            if(episode == null) {
                episode = currentSelectedEpisode.value!!.getNextEpisode()
            }

            videoDataProvider(
                film = watchHistoryItem.value.film,
                mediaId = videoData.value.mediaId,
                server = appSettings.value.preferredServer,
                watchHistoryItem = watchHistoryItem.value,
                episode = episode,
                onSuccess = { newData, newEpisode ->
                    videoTrackGroup = null
                    qualityTrackGroups.clear()
                    availableQualities.clear()
                    availableSubtitles.clear()

                    savedStateHandle[VIDEO_DATA] = newData
                    savedStateHandle[EPISODE_SELECTED] = newEpisode

                    _uiState.update {
                        PlayerUiState(
                            selectedServer = it.selectedServer,
                            selectedPlaybackSpeedIndex = it.selectedPlaybackSpeedIndex,
                            screenBrightness = it.screenBrightness,
                            currentTime = getLastWatchTime(
                                watchHistoryItem.value,
                                currentSelectedEpisode.value
                            ),
                            selectedSubtitle = newData.getDefaultSubtitleIndex()
                        )
                    }

                    startExtractingSubtitles()
                    val subtitleToUse = when {
                        !appSettings.value.isSubtitleEnabled -> 0
                        else -> newData.getDefaultSubtitleIndex()
                    }
                    updateSubtitle(subtitleToUse)

                }
            ).collectLatest { state ->
                _dialogState.update { state }
            }
        }
    }

    private suspend fun TMDBEpisode.getNextEpisode(): TMDBEpisode {
        val nextEpisode: TMDBEpisode
        var seasonToUse = _season.value!!.data

        val episodeSeasonNumber = this.season
        val episodeNumber = this.episode

        val currentLoadedSeasonNumber = seasonToUse?.seasonNumber
        if(currentLoadedSeasonNumber != episodeSeasonNumber) {
            seasonToUse = withContext(defaultDispatcher) {
                fetchSeasonFromProvider(seasonNumber = episodeSeasonNumber)
            }
        }

        val episodesList = seasonToUse!!.episodes
        val nextEpisodeNumberToWatch = episodeNumber + 1

        if(nextEpisodeNumberToWatch <= episodesList.size) {
            nextEpisode = episodesList.find {
                it.episode == nextEpisodeNumberToWatch
            } ?: throw NullPointerException("Episode cannot be null!")
        } else if(seasonToUse.seasonNumber + 1 <= seasonCount.value!!) {
            seasonToUse = fetchSeasonFromProvider(seasonNumber = currentSelectedEpisode.value!!.season + 1)
            _season.update { Resource.Success(seasonToUse!!) }

            nextEpisode = seasonToUse!!.episodes[0]
        } else throw NullPointerException("Episode cannot be null!")

        return nextEpisode
    }

    fun onSeasonChange(seasonNumber: Int) {
        if(onSeasonChangeJob?.isActive == true)
            return

        onSeasonChangeJob = viewModelScope.launch {
            _season.update { Resource.Loading }
            _season.update {
                val result = fetchSeasonFromProvider(seasonNumber)
                if(result == null)
                    Resource.Failure("Could not fetch season data")
                else {
                    Resource.Success(result)
                }
            }
        }
    }

    /**
     * Resets the player dialog uiState to idle.
     */
    fun onConsumePlayerDialog() {
        _dialogState.update { VideoDataDialogState.Idle }
    }

    fun updateScreenBrightness(strength: Float) {
        _uiState.update {
            it.copy(screenBrightness = strength)
        }
    }

    private fun updateSubtitle(subtitle: Int) {
        _uiState.update {
            it.copy(selectedSubtitle = subtitle)
        }
    }

    private fun updateVideoQuality(quality: Int) {
        _uiState.update {
            it.copy(selectedQuality = quality)
        }
    }

    private fun updateServerSelected(index: Int? = null) {
        val serverIndex: Int
        if(index == null) {
            serverIndex = videoData.value.servers?.indexOfFirst { server ->
                server.serverName == appSettings.value.preferredServer
            } ?: -1

            if(serverIndex == -1)
                return
        } else serverIndex = index

        _uiState.update {
            it.copy(selectedServer = serverIndex)
        }
    }

    fun updateCurrentTime(time: Long?) {
        time?.let {
            if(!_uiState.value.isPlaying)
                return

            _uiState.update {
                it.copy(currentTime = time)
            }
        }
    }

    fun updateIsPlayingState(isPlaying: Boolean = !_uiState.value.isPlaying) {
        _uiState.update {
            it.copy(isPlaying = isPlaying)
        }
        updateWatchHistory()
    }

    fun updateWatchHistory() {
        viewModelScope.launch {
            val currentTime = _uiState.value.currentTime
            val minute = 60000

            if(currentTime <= minute)
                return@launch

            val totalDuration = _uiState.value.totalDuration
            savedStateHandle[WATCH_HISTORY_ITEM] = watchHistoryItemManager.updateWatchHistoryItem(
                watchHistoryItem = watchHistoryItem.value,
                currentTime = currentTime,
                totalDuration = totalDuration,
                currentSelectedEpisode = currentSelectedEpisode.value
            )
        }
    }

    private suspend fun fetchSeasonFromProvider(seasonNumber: Int): Season? {
        val filmId = watchHistoryItem.value.film.id
        val seasonToUse = seasonProvider(id = filmId, seasonNumber = seasonNumber)
        if(seasonToUse != null) {
            watchHistoryItemManager
                .updateEpisodeCount(
                    id = filmId,
                    seasonNumber = seasonToUse.seasonNumber,
                    episodeCount = seasonToUse.episodes.size
                )?.let {
                    savedStateHandle[WATCH_HISTORY_ITEM] = it
                }
        }

        return seasonToUse
    }

    fun showSnackbar(message: String, type: PlayerSnackbarMessageType) {
        if(onShowSnackbarJob?.isActive == true)
            return

        onShowSnackbarJob = viewModelScope.launch {
            val itemIndexInQueue = withContext(defaultDispatcher) {
                snackbarQueue.indexOfFirst { it.type == type }
            }
            val isSameTypeAlreadyQueued = itemIndexInQueue != -1

            if(!isSameTypeAlreadyQueued) {
                snackbarQueue.add(PlayerSnackbarMessage(message, type))
                return@launch
            }

            when(type) {
                PlayerSnackbarMessageType.Episode -> {
                    val itemInQueue = snackbarQueue[itemIndexInQueue]
                    snackbarQueue[itemIndexInQueue] = itemInQueue.copy(
                        message = message
                    )
                }
                else -> {
                    snackbarQueue.removeAt(itemIndexInQueue)
                    delay(700)
                    snackbarQueue.add(PlayerSnackbarMessage(message, type))
                }
            }
        }
    }

    fun removeSnackbar(position: Int) {
        if(onRemoveSnackbarJob?.isActive == true)
            return

        onRemoveSnackbarJob = viewModelScope.launch {
            if(position > snackbarQueue.size - 1)
                return@launch

            snackbarQueue.removeAt(position)
        }
    }
}