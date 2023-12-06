package com.flixclusive.presentation.tv.screens.player

import androidx.compose.runtime.mutableStateListOf
import androidx.core.net.toUri
import androidx.datastore.core.DataStore
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.TrackGroup
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import com.flixclusive.domain.model.VideoDataDialogState
import com.flixclusive.domain.model.entities.WatchHistoryItem
import com.flixclusive.domain.model.tmdb.Film
import com.flixclusive.domain.model.tmdb.Season
import com.flixclusive.domain.model.tmdb.TMDBEpisode
import com.flixclusive.domain.model.tmdb.TvShow
import com.flixclusive.domain.preferences.AppSettings
import com.flixclusive.domain.repository.WatchHistoryRepository
import com.flixclusive.domain.usecase.SeasonProviderUseCase
import com.flixclusive.domain.usecase.VideoDataProviderUseCase
import com.flixclusive.domain.usecase.WatchHistoryItemManagerUseCase
import com.flixclusive.domain.utils.FilmProviderUtils.getSubtitleIndex
import com.flixclusive.presentation.common.PlayerUiState
import com.flixclusive.presentation.common.viewmodels.film.FilmScreenNavArgs
import com.flixclusive.presentation.navArgs
import com.flixclusive.providers.models.common.VideoData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@UnstableApi
@HiltViewModel
class TvPlayerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val videoDataProvider: VideoDataProviderUseCase,
    private val _appSettings: DataStore<AppSettings>,
    private val watchHistoryRepository: WatchHistoryRepository,
    private val watchHistoryItemManager: WatchHistoryItemManagerUseCase,
    private val seasonProvider: SeasonProviderUseCase,
) : ViewModel() {
    private val filmArgs = savedStateHandle.navArgs<FilmScreenNavArgs>()

    val appSettings = _appSettings.data
        .drop(1)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = runBlocking { _appSettings.data.first() }
        )

    val watchHistoryItem = watchHistoryRepository.getWatchHistoryItemByIdInFlow(itemId = filmArgs.film.id)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = WatchHistoryItem()
        )

    private val _currentSelectedEpisode = MutableStateFlow<TMDBEpisode?>(null)
    val currentSelectedEpisode = _currentSelectedEpisode.asStateFlow()

    private val _videoData = MutableStateFlow<VideoData?>(null)
    val videoData = _videoData.asStateFlow()

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState = _uiState.asStateFlow()

    private val _dialogState = MutableStateFlow<VideoDataDialogState>(VideoDataDialogState.Idle)
    val dialogState = _dialogState.asStateFlow()

    private var season: Season? = null // If it is a TV Show
    private var seasonCount: Int? = null // If it is a TV Show

    private var videoTrackGroup: TrackGroup? = null
    private val qualityTrackGroups = mutableListOf<Int>()
    val availableQualities = mutableStateListOf<String>()
    val availableSubtitles = mutableStateListOf<MediaItem.SubtitleConfiguration>()

    private var onPlayClickJob: Job? = null

    init {
        updateServerSelected()
        startExtractingSubtitles()
    }

    fun play(film: Film, episodeToPlay: TMDBEpisode? = null) {
        if(onPlayClickJob?.isActive == true) {
            onPlayClickJob?.cancel()
        }

        onPlayClickJob = viewModelScope.launch {
            if(film is TvShow) {
                seasonCount = film.totalSeasons
            }

            var episodeToUse = episodeToPlay
            if(episodeToUse == null) {
                episodeToUse = _currentSelectedEpisode.value?.getNextEpisode()
            }

            videoDataProvider(
                film = film,
                mediaId = _videoData.value?.mediaId,
                watchHistoryItem = watchHistoryItem.value,
                episode = episodeToUse,
                onSuccess = { newData, newEpisode ->
                    _uiState.update {
                        PlayerUiState(
                            selectedServer = it.selectedServer,
                            selectedPlaybackSpeedIndex = it.selectedPlaybackSpeedIndex,
                            screenBrightness = it.screenBrightness
                        )
                    }
                    updateSubtitle(newData.getSubtitleIndex())
                    getLastWatchTime(newEpisode)

                    _videoData.update { newData }
                    _currentSelectedEpisode.update { newEpisode }
                }
            ).collectLatest { state ->
                _dialogState.update { state }
            }
        }
    }

    private fun getLastWatchTime(episodeToWatch: TMDBEpisode?) {
        if(watchHistoryItem.value?.episodesWatched?.isEmpty() == true)
            return

        val isTvShow = watchHistoryItem.value?.seasons != null
        val currentTimeToUse = if (isTvShow) {
            val episodeToUse = watchHistoryItem.value?.episodesWatched?.find {
                it.seasonNumber == episodeToWatch?.season
                    && it.episodeNumber == episodeToWatch?.episode
            }

            if(episodeToUse?.isFinished == true) 0L
            else episodeToUse?.watchTime ?: 0L
        } else if(watchHistoryItem.value?.episodesWatched?.last()?.isFinished == false) {
            watchHistoryItem.value?.episodesWatched?.last()?.watchTime ?: 0L
        } else {
            0L
        }

        _uiState.update {
            it.copy(currentTime = currentTimeToUse)
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

    private fun updateServerSelected(index: Int = 0) {
        _uiState.update {
            it.copy(selectedServer = index)
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
            watchHistoryItemManager.updateWatchHistoryItem(
                watchHistoryItem = watchHistoryItem.value ?: WatchHistoryItem(),
                currentTime = _uiState.value.currentTime,
                totalDuration = _uiState.value.totalDuration,
            )
        }
    }

    private fun startExtractingSubtitles() {
        viewModelScope.launch {
            _videoData.collectLatest {
                availableSubtitles.clear()
                it?.subtitles?.forEach { subtitle ->
                    val mimeType = when (subtitle.url.split(".").last()) {
                        "vtt" -> MimeTypes.TEXT_VTT
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
                    .setPreferredTextLanguage(_videoData.value?.subtitles?.get(_uiState.value.selectedSubtitle)?.lang?.lowercase())
                    .build()
            }
        }
    }

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

    suspend fun initializeWatchItemManager(seasonNumber: Int) {
        val filmId = watchHistoryItem.value?.film?.id
        val seasonToUse = filmId?.let {
            seasonProvider(id = it, seasonNumber = seasonNumber)
        }
        if(seasonToUse != null) {
            watchHistoryItemManager
                .updateEpisodeCount(
                    id = filmId,
                    seasonNumber = seasonToUse.seasonNumber,
                    episodeCount = seasonToUse.episodes.size
                )
        }
    }

    private suspend fun TMDBEpisode.getNextEpisode(): TMDBEpisode {
        val nextEpisode: TMDBEpisode

        val episodeSeasonNumber = season
        val episodeNumber = episode

        var seasonToUse = this@TvPlayerViewModel.season ?: seasonProvider(
            id = filmArgs.film.id,
            seasonNumber = episodeSeasonNumber
        )

        val episodesList = seasonToUse!!.episodes
        val nextEpisodeNumberToWatch = episodeNumber + 1

        if(episodesList.last().episode == nextEpisodeNumberToWatch)
            return episodesList.last()

        nextEpisode = if (episodesList.last().episode > nextEpisodeNumberToWatch) {
            episodesList.find {
                it.episode == nextEpisodeNumberToWatch
            } ?: throw NullPointerException("Episode $nextEpisodeNumberToWatch doesn't exist on Season ${seasonToUse.seasonNumber}")
        } else if (seasonToUse.seasonNumber + 1 <= seasonCount!!) {
            seasonToUse = seasonProvider(
                id = filmArgs.film.id,
                seasonNumber = currentSelectedEpisode.value!!.season + 1
            )

            this@TvPlayerViewModel.season = seasonToUse

            seasonToUse!!.episodes[0]
        } else throw NullPointerException("Episode $nextEpisodeNumberToWatch doesn't exist on Season ${seasonToUse.seasonNumber}")

        return nextEpisode
    }
}
