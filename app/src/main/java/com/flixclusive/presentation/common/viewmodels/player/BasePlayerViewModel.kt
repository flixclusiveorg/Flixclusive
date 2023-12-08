package com.flixclusive.presentation.common.viewmodels.player

import androidx.compose.runtime.mutableStateListOf
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.Tracks
import com.flixclusive.domain.model.VideoDataDialogState
import com.flixclusive.domain.model.entities.WatchHistoryItem
import com.flixclusive.domain.model.tmdb.Film
import com.flixclusive.domain.model.tmdb.Season
import com.flixclusive.domain.model.tmdb.TMDBEpisode
import com.flixclusive.domain.preferences.AppSettingsManager
import com.flixclusive.domain.usecase.VideoDataProviderUseCase
import com.flixclusive.domain.utils.FilmProviderUtils.getSubtitleIndex
import com.flixclusive.domain.utils.WatchHistoryUtils
import com.flixclusive.presentation.common.PlayerUiState
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
import java.util.Locale

@Suppress("PropertyName")
abstract class BasePlayerViewModel(
    appSettingsManager: AppSettingsManager,
    private val videoDataProvider: VideoDataProviderUseCase,
) : ViewModel()  {
    val sources = videoDataProvider.providers

    private val _dialogState = MutableStateFlow<VideoDataDialogState>(VideoDataDialogState.Idle)
    val dialogState: StateFlow<VideoDataDialogState> = _dialogState.asStateFlow()

    protected val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private val _videoData = MutableStateFlow(VideoData())
    open val videoData = _videoData.asStateFlow()

    abstract val watchHistoryItem: StateFlow<WatchHistoryItem?>

    private val _currentSelectedEpisode = MutableStateFlow<TMDBEpisode?>(null)
    open val currentSelectedEpisode = _currentSelectedEpisode.asStateFlow()

    val appSettings = appSettingsManager.appSettings
        .data
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = appSettingsManager.localAppSettings
        )

    private var onLoadVideoSource: Job? = null


    // == CCs/Audios/Qualities
    private val qualityTrackGroups: MutableList<Tracks.Group?> = mutableListOf()
    private val audioTrackGroups: MutableList<Tracks.Group?> = mutableListOf()
    val availableAudios = mutableStateListOf<String>()
    val availableQualities = mutableStateListOf<String>()
    val availableSubtitles = mutableStateListOf<MediaItem.SubtitleConfiguration>()

    protected fun clearTracks() {
        audioTrackGroups.clear()
        qualityTrackGroups.clear()

        availableAudios.clear()
        availableQualities.clear()
        availableSubtitles.clear()
    }

    open fun initialize() {
        clearTracks()

        _uiState.update {
            val watchHistoryItem = watchHistoryItem.value ?: WatchHistoryItem()

            PlayerUiState(
                currentTime = WatchHistoryUtils.getLastWatchTime(
                    watchHistoryItem,
                    currentSelectedEpisode.value
                ),
                totalDuration = WatchHistoryUtils.getTotalDuration(
                    watchHistoryItem,
                    currentSelectedEpisode.value
                ),
                selectedSource = videoData.value.sourceName,
            )
        }

        extractSubtitles()
        val subtitleToUse = when {
            !appSettings.value.isSubtitleEnabled -> 0
            else -> videoData.value.getSubtitleIndex(appSettings.value.subtitleLanguage)
        }

        updateServerSelected()
        updateSubtitle(subtitleToUse)
    }

    // =======================
    protected open fun onSuccessCallback(newData: VideoData) {
        clearTracks()

        val subtitleToUse = when {
            !appSettings.value.isSubtitleEnabled -> 0
            else -> newData.getSubtitleIndex(appSettings.value.subtitleLanguage)
        }
        updateSubtitle(subtitleToUse)
        _uiState.update { it.copy(selectedAudio = 0) }
    }

    protected abstract fun onErrorCallback(message: String? = null)

    protected fun onServerChange(film: Film, serverIndex: Int) {
        if (onLoadVideoSource?.isActive == true)
            return

        onLoadVideoSource = viewModelScope.launch {
            val oldSelectedServer = _uiState.value.selectedServer

            updateServerSelected(serverIndex)
            val server = videoData.value.servers!![serverIndex]

            if(!server.isEmbed) {
                return@launch onSuccessCallback(
                    videoData.value.copy(source = server.serverUrl)
                )
            }

            videoDataProvider(
                film = film,
                mediaId = videoData.value.mediaId,
                server = server.serverName,
                source = _uiState.value.selectedSource,
                watchHistoryItem = watchHistoryItem.value,
                episode = currentSelectedEpisode.value,
                onSuccess = { newData, _ ->
                    onSuccessCallback(newData)
                },
                onError = {
                    updateServerSelected(oldSelectedServer)
                    onErrorCallback("Failed to retrieve server [${server.serverName}]")
                }
            ).collect()
        }
    }

    protected fun onSourceChange(film: Film, newSource: String) {
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
                onSuccess = { newData, _ ->
                    onSuccessCallback(newData)
                },
                onError = {
                    updateSourceSelected(oldSelectedSource)
                    onErrorCallback("Failed to retrieve source [$newSource]")
                }
            ).collect()
        }
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
    private fun extractSubtitles() {
        viewModelScope.launch {
            videoData.collectLatest {
                availableSubtitles.clear()
                it.subtitles.forEach { subtitle ->
                    val mimeType = when {
                        subtitle.url.endsWith(".vtt", true) -> MimeTypes.TEXT_VTT
                        subtitle.url.endsWith(".srt", true) -> MimeTypes.APPLICATION_SUBRIP
                        else -> null
                    }

                    val subtitleConfiguration = MediaItem.SubtitleConfiguration
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
     * Extracts the source qualities by populating
     * the available qualities and track groups.
     *
     * @return The selected quality track
     */
    fun extractQualities(tracks: Tracks): Int {
        if (qualityTrackGroups.isEmpty()) {
            qualityTrackGroups.addAll(
                tracks.groups
                    .filter { group ->
                        group.type == C.TRACK_TYPE_VIDEO
                    }
            )

            qualityTrackGroups.forEach { group ->
                group?.let {
                    for (trackIndex in 0 until it.length) {
                        availableQualities.add("${it.getTrackFormat(trackIndex).height}p")
                    }
                }
            }

            if (availableQualities.size > 1) {
                availableQualities.add(
                    index = 0,
                    element = "Auto"
                )
            }

            val preferredQuality =
                availableQualities.indexOfFirst {
                    appSettings.value.preferredQuality.equals(
                        other = it, ignoreCase = true
                    )
                }

            if(preferredQuality != -1) {
                _uiState.update {
                    it.copy(selectedQuality = preferredQuality)
                }
            }
        }

        return _uiState.value.selectedQuality
    }

    /**
     * Extracts the source audios by populating
     * the available audios and track groups.
     *
     * @return The selected audio track
     */
    fun extractAudios(tracks: Tracks): Int {
        if (audioTrackGroups.isEmpty()) {
            audioTrackGroups.addAll(
                tracks.groups
                    .filter { group ->
                        group.type == C.TRACK_TYPE_AUDIO
                    }
            )

            audioTrackGroups.forEach { group ->
                group?.let {
                    for (trackIndex in 0 until it.length) {
                        val format = it.getTrackFormat(trackIndex)
                        availableAudios.add("Audio Track #${availableAudios.size + 1}: ${Locale(format.language ?: "Unknown").displayLanguage}")
                    }
                }
            }
        }

        return _uiState.value.selectedAudio
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
        _uiState.update {
            it.copy(selectedQuality = qualityIndex)
        }

        return qualityTrackGroups[0]?.let { group ->
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
                            TrackSelectionOverride(group.mediaTrackGroup, qualityIndex - 1)
                        )
                        .build()
                }
            }
        }
    }

    /**
     * Callback function triggered when the audio track is changed.
     *
     * @param audioIndex The index of the selected audio track.
     */
    fun onAudioChange(
        audioIndex: Int,
        trackParameters: TrackSelectionParameters,
    ): TrackSelectionParameters? {
        _uiState.update {
            it.copy(selectedAudio = audioIndex)
        }

        return audioTrackGroups[audioIndex]?.let { group ->
            trackParameters
                .buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
                .setPreferredAudioLanguage(group.getTrackFormat(0).language ?: "en")
                .build()
        }
    }

    fun onPlaybackSpeedChange(speedIndex: Int) {
        _uiState.update { it.copy(selectedPlaybackSpeedIndex = speedIndex) }
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

    fun updateScreenBrightness(strength: Float) {
        _uiState.update {
            it.copy(screenBrightness = strength)
        }
    }

    protected fun updateSubtitle(subtitle: Int) {
        _uiState.update {
            it.copy(selectedSubtitle = subtitle)
        }
    }

    protected fun updateServerSelected(index: Int? = null) {
        val indexToUse = index
            ?: videoData.value.servers?.indexOfFirst {
                it.serverName.contains(appSettings.value.preferredQuality, true)
                        || it.serverName == appSettings.value.preferredQuality
            } ?: 0

        _uiState.update {
            it.copy(selectedServer = if (indexToUse == -1) 0 else indexToUse)
        }
    }

    private fun updateSourceSelected(source: String?) {
        _uiState.update {
            it.copy(selectedSource = source)
        }
    }

    fun updateCurrentTime(time: Long?) {
        time?.let {
            if (!_uiState.value.isPlaying)
                return

            _uiState.update {
                it.copy(currentTime = time)
            }
        }
    }

    fun updateIsPlayingState(isPlaying: Boolean? = null) {
        _uiState.update {
            it.copy(isPlaying = isPlaying ?: !it.isPlaying)
        }
        updateWatchHistory()
    }

    abstract fun updateWatchHistory()

    protected abstract suspend fun TMDBEpisode.fetchSeasonIfNeeded(seasonNumber: Int): Season?

    private suspend fun TMDBEpisode.getNextEpisode(
        seasonCount: Int,
        updateSeason: ((Season?) -> Unit)? = null
    ): TMDBEpisode {
        val nextEpisode: TMDBEpisode

        val episodeSeasonNumber = this.season
        val episodeNumber = this.episode

        var seasonToUse = fetchSeasonIfNeeded(episodeSeasonNumber)

        val episodesList = seasonToUse!!.episodes
        val nextEpisodeNumberToWatch = episodeNumber + 1

        if(episodesList.last().episode == nextEpisodeNumberToWatch)
            return episodesList.last()

        nextEpisode = if (episodesList.last().episode > nextEpisodeNumberToWatch) {
            episodesList.find {
                it.episode == nextEpisodeNumberToWatch
            } ?: throw NullPointerException("Episode $nextEpisodeNumberToWatch doesn't exist on Season ${seasonToUse.seasonNumber}")
        } else if (seasonToUse.seasonNumber + 1 <= seasonCount) {
            seasonToUse = fetchSeasonIfNeeded(seasonNumber = currentSelectedEpisode.value!!.season + 1)

            updateSeason?.invoke(seasonToUse)

            seasonToUse!!.episodes.first()
        } else throw NullPointerException("Episode $nextEpisodeNumberToWatch doesn't exist on Season ${seasonToUse.seasonNumber}")

        return nextEpisode
    }

    /**
     * Callback function triggered when an episode is clicked.
     *
     * @param episodeToWatch The next episode to be played, or null if not available.
     */
    fun loadVideoData(
        film: Film,
        seasonCount: Int,
        episodeToWatch: TMDBEpisode? = null,
        onSuccess: (VideoData, TMDBEpisode?) -> Unit,
        updateSeason: ((Season?) -> Unit)? = null
    ) {
        if (onLoadVideoSource?.isActive == true)
            return

        onLoadVideoSource = viewModelScope.launch {
            var episode = episodeToWatch
            if (episode == null) {
                episode = currentSelectedEpisode.value!!.getNextEpisode(
                    seasonCount = seasonCount,
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
                    clearTracks()

                    _uiState.update {
                        PlayerUiState(
                            selectedServer = it.selectedServer,
                            selectedPlaybackSpeedIndex = it.selectedPlaybackSpeedIndex,
                            selectedSource = it.selectedSource,
                            screenBrightness = it.screenBrightness,
                            currentTime = WatchHistoryUtils.getLastWatchTime(
                                watchHistoryItem.value ?: WatchHistoryItem(),
                                newEpisode
                            )
                        )
                    }

                    val subtitleToUse = when {
                        !appSettings.value.isSubtitleEnabled -> 0
                        else -> newData.getSubtitleIndex(appSettings.value.subtitleLanguage)
                    }
                    updateSubtitle(subtitleToUse)

                    onSuccess(newData, newEpisode)
                }
            ).collectLatest { state ->
                _dialogState.update { state }
            }
        }
    }


    protected abstract suspend fun fetchSeasonFromProvider(showId: Int, seasonNumber: Int): Season?
}