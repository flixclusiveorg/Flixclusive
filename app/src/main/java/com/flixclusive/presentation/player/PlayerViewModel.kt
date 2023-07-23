package com.flixclusive.presentation.player

import androidx.compose.runtime.mutableStateListOf
import androidx.core.net.toUri
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
import com.flixclusive.domain.model.consumet.VideoData
import com.flixclusive.domain.model.entities.WatchHistoryItem
import com.flixclusive.domain.model.tmdb.Season
import com.flixclusive.domain.model.tmdb.TMDBEpisode
import com.flixclusive.domain.preferences.VideoDataServerPreferences
import com.flixclusive.domain.usecase.SeasonProviderUseCase
import com.flixclusive.domain.usecase.VideoDataProviderUseCase
import com.flixclusive.domain.usecase.WatchHistoryItemManagerUseCase
import com.flixclusive.domain.utils.ConsumetUtils.getDefaultSubtitleIndex
import com.flixclusive.presentation.common.VideoDataDialogState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@UnstableApi
@HiltViewModel
class PlayerViewModel @Inject constructor(
    @DefaultDispatcher val defaultDispatcher: CoroutineDispatcher,
    private val videoDataProvider: VideoDataProviderUseCase,
    private val videoDataServerPreferences: VideoDataServerPreferences,
    private val seasonProvider: SeasonProviderUseCase,
    private val watchHistoryItemManager: WatchHistoryItemManagerUseCase,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val snackbarQueue = mutableStateListOf<PlayerSnackbarMessage>()

    private val _dialogState = MutableStateFlow(VideoDataDialogState.IDLE)
    val dialogState: StateFlow<VideoDataDialogState> = _dialogState.asStateFlow()

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    val videoData = savedStateHandle.getStateFlow(VIDEO_DATA, VideoData())
    var watchHistoryItem = savedStateHandle.get<WatchHistoryItem>(WATCH_HISTORY_ITEM) ?: WatchHistoryItem()

    // Only valid if video data is a tv show
    val seasonCount = savedStateHandle.get<Int?>(SEASON_COUNT)
    val currentSelectedEpisode = savedStateHandle.getStateFlow<TMDBEpisode?>(EPISODE_SELECTED, null)
    private val _season = MutableStateFlow<Resource<Season>?>(null)
    val season = _season.asStateFlow()

    private val preferredServer = videoDataServerPreferences.getPreferredServer
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    private var videoTrackGroup: TrackGroup? = null
    private val qualityTrackGroups = mutableListOf<Int>()
    val availableQualities = mutableStateListOf<String>()
    val availableSubtitles = mutableStateListOf<SubtitleConfiguration>()

    private var onSeasonChangeJob: Job? = null
    private var onShowSnackbarJob: Job? = null
    private var onRemoveSnackbarJob: Job? = null

    init {
        viewModelScope.launch {
            preferredServer.collectLatest { preferredServerName ->
                val selectedServerIndex = videoData.value.servers?.indexOfFirst { server ->
                    server.serverName == preferredServerName
                } ?: -1

                if(selectedServerIndex == -1)
                    return@collectLatest

                updateServerSelected(server = selectedServerIndex)
            }
        }

        viewModelScope.launch {
            if (currentSelectedEpisode.value != null) {
                if(currentSelectedEpisode.value!!.season != seasonCount) {
                    fetchSeasonFromProvider(seasonCount!!)
                }

                onSeasonChange(currentSelectedEpisode.value!!.season)
            }
        }

        startExtractingSubtitles()
        updateSubtitle(videoData.value.getDefaultSubtitleIndex())
        getLastWatchTime(currentSelectedEpisode.value)
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
                playbackState = Player.STATE_IDLE
            )
        }
    }

    private fun getLastWatchTime(episodeToWatch: TMDBEpisode?) {
        if(watchHistoryItem.episodesWatched.isEmpty())
            return

        val isTvShow = watchHistoryItem.seasons != null
        val currentTimeToUse = if (isTvShow) {
            val episodeToUse = watchHistoryItem.episodesWatched.find {
                it.seasonNumber == episodeToWatch?.season
                    && it.episodeNumber == episodeToWatch?.episode
            }

            if(episodeToUse?.isFinished == true) 0L
            else episodeToUse?.watchTime ?: 0L
        } else if(!watchHistoryItem.episodesWatched.last().isFinished) {
            watchHistoryItem.episodesWatched.last().watchTime
        } else {
            0L
        }

        _uiState.update {
            it.copy(currentTime = currentTimeToUse)
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

            val isAutoQualityAvailable = videoData.value.sources.find {
                it.quality.contains(
                    "auto",
                    ignoreCase = true
                )
            } != null
            if (isAutoQualityAvailable) {
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
                    .setPreferredTextLanguage(videoData.value.subtitles[_uiState.value.selectedSubtitle].lang.lowercase())
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
        viewModelScope.launch {
            val selectedServer = videoData.value.servers!![serverIndex].serverName
            videoDataServerPreferences.savePreferredServer(serverName = selectedServer)

            videoDataProvider(
                film = watchHistoryItem.film,
                consumetId = videoData.value.mediaId,
                server = selectedServer,
                watchHistoryItem = watchHistoryItem,
                episode = currentSelectedEpisode.value,
                onSuccess = { newData, _ ->
                    videoTrackGroup = null
                    qualityTrackGroups.clear()
                    availableQualities.clear()
                    _uiState.update { it.copy(selectedQuality = 0) }

                    savedStateHandle[VIDEO_DATA] = newData
                }
            ).collectLatest {}
        }
    }

    /**
     * Callback function triggered when an episode is clicked.
     *
     * @param episodeToWatch The next episode to be played, or null if not available.
     */
    suspend fun onEpisodeClick(episodeToWatch: TMDBEpisode? = null) {
        var episode = episodeToWatch
        if(episode == null) {
            episode = currentSelectedEpisode.value!!.getNextEpisode()
        }

        withContext(defaultDispatcher) {
            videoDataProvider(
                film = watchHistoryItem.film,
                consumetId = videoData.value.mediaId,
                server = preferredServer.value,
                watchHistoryItem = watchHistoryItem,
                episode = episode,
                onSuccess = { newData, episode ->
                    _uiState.update { PlayerUiState(selectedServer = it.selectedServer) }
                    updateSubtitle(newData.getDefaultSubtitleIndex())
                    getLastWatchTime(episode)

                    savedStateHandle[VIDEO_DATA] = newData
                    savedStateHandle[EPISODE_SELECTED] = episode

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
        } else if(seasonToUse.seasonNumber + 1 <= seasonCount!!) {
            seasonToUse = withContext(defaultDispatcher) {
                fetchSeasonFromProvider(seasonNumber = currentSelectedEpisode.value!!.season + 1)
            }

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
     * Resets the player dialog state to idle.
     */
    fun onConsumePlayerDialog() {
        _dialogState.update { VideoDataDialogState.IDLE }
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

    private fun updateServerSelected(server: Int) {
        _uiState.update {
            it.copy(selectedServer = server)
        }
    }

    fun updateCurrentTime(time: Long?) {
        time?.let {
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
            watchHistoryItem = watchHistoryItemManager.updateWatchHistoryItem(
                watchHistoryItem = watchHistoryItem,
                currentTime = currentTime,
                totalDuration = totalDuration,
                currentSelectedEpisode = currentSelectedEpisode.value
            )
        }
    }

    private suspend fun fetchSeasonFromProvider(seasonNumber: Int): Season? {
        val filmId = watchHistoryItem.film.id
        val seasonToUse = seasonProvider(id = filmId, seasonNumber = seasonNumber)
        if(seasonToUse != null) {
            watchHistoryItemManager
                .updateEpisodeCount(
                    id = filmId,
                    seasonNumber = seasonToUse.seasonNumber,
                    episodeCount = seasonToUse.episodes.size
                )?.let {
                    watchHistoryItem = it
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