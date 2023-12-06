package com.flixclusive.presentation.mobile.screens.player

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.flixclusive.domain.common.Resource
import com.flixclusive.domain.model.entities.WatchHistoryItem
import com.flixclusive.domain.model.tmdb.Season
import com.flixclusive.domain.model.tmdb.TMDBEpisode
import com.flixclusive.domain.preferences.AppSettingsManager
import com.flixclusive.domain.usecase.SeasonProviderUseCase
import com.flixclusive.domain.usecase.VideoDataProviderUseCase
import com.flixclusive.domain.usecase.WatchHistoryItemManagerUseCase
import com.flixclusive.presentation.common.viewmodels.player.BasePlayerViewModel
import com.flixclusive.providers.models.common.VideoData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@UnstableApi
@HiltViewModel
class PlayerViewModel @Inject constructor(
    videoDataProvider: VideoDataProviderUseCase,
    appSettingsManager: AppSettingsManager,
    private val seasonProvider: SeasonProviderUseCase,
    private val watchHistoryItemManager: WatchHistoryItemManagerUseCase,
    val savedStateHandle: SavedStateHandle,
) : BasePlayerViewModel(
    appSettingsManager = appSettingsManager,
    videoDataProvider = videoDataProvider
) {
    override val videoData = savedStateHandle.getStateFlow(VIDEO_DATA, VideoData())
    override val watchHistoryItem =
        savedStateHandle.getStateFlow(WATCH_HISTORY_ITEM, WatchHistoryItem())
    override val currentSelectedEpisode =
        savedStateHandle.getStateFlow<TMDBEpisode?>(EPISODE_SELECTED, null)

    val snackbarQueue = mutableStateListOf<PlayerSnackbarMessage>()

    // Only valid if video data is a tv show
    val seasonCount = savedStateHandle.getStateFlow<Int?>(SEASON_COUNT, null)

    private val _season = MutableStateFlow<Resource<Season>?>(null)
    val season = _season.asStateFlow()
    // =====================================

    private var onSeasonChangeJob: Job? = null
    private var onShowSnackbarJob: Job? = null
    private var onRemoveSnackbarJob: Job? = null

    init {
        initialize()
    }

    override fun initialize() {
        viewModelScope.launch {
            if (currentSelectedEpisode.value != null) {
                if (currentSelectedEpisode.value!!.season != seasonCount.value) {
                    fetchSeasonFromProvider(
                        showId = watchHistoryItem.value.id,
                        seasonNumber = seasonCount.value!!
                    )
                }

                onSeasonChange(currentSelectedEpisode.value!!.season)
            }
        }

        super.initialize()
    }

    override fun onSuccessCallback(newData: VideoData) {
        super.onSuccessCallback(newData)
        savedStateHandle[VIDEO_DATA] = newData
    }

    override fun onErrorCallback(message: String?) {
        showSnackbar(
            message = message ?: "Unknown error occured",
            type = PlayerSnackbarMessageType.Error
        )
    }

    override fun updateWatchHistory() {
        viewModelScope.launch {
            savedStateHandle[WATCH_HISTORY_ITEM] = watchHistoryItemManager.updateWatchHistoryItem(
                watchHistoryItem = watchHistoryItem.value,
                currentTime = _uiState.value.currentTime,
                totalDuration = _uiState.value.totalDuration,
                currentSelectedEpisode = currentSelectedEpisode.value
            )
        }
    }


    override suspend fun TMDBEpisode.fetchSeasonIfNeeded(seasonNumber: Int): Season? {
        var seasonToUse = _season.value!!.data

        val currentLoadedSeasonNumber = seasonToUse?.seasonNumber
        if (currentLoadedSeasonNumber != seasonNumber) {
            seasonToUse = fetchSeasonFromProvider(
                showId = watchHistoryItem.value.id,
                seasonNumber = seasonNumber
            )
        }

        return seasonToUse
    }

    override suspend fun fetchSeasonFromProvider(showId: Int, seasonNumber: Int): Season? {
        val seasonToUse = seasonProvider(id = showId, seasonNumber = seasonNumber)
        if (seasonToUse != null) {
            watchHistoryItemManager
                .updateEpisodeCount(
                    id = showId,
                    seasonNumber = seasonToUse.seasonNumber,
                    episodeCount = seasonToUse.episodes.size
                )?.let {
                    savedStateHandle[WATCH_HISTORY_ITEM] = it
                }
        }

        return seasonToUse
    }

    fun onServerChange(serverIndex: Int) {
        onServerChange(
            film = watchHistoryItem.value.film,
            serverIndex = serverIndex
        )
    }

    fun onSourceChange(newSource: String) {
        onSourceChange(
            film = watchHistoryItem.value.film,
            newSource = newSource
        )
    }

    /**
     * Callback function triggered when an episode is clicked.
     *
     * @param episodeToWatch The next episode to be played, or null if not available.
     */
    fun onEpisodeClick(episodeToWatch: TMDBEpisode? = null) {
        loadVideoData(
            film = watchHistoryItem.value.film,
            seasonCount = seasonCount.value!!,
            episodeToWatch = episodeToWatch,
            onSuccess = { newData, newEpisode ->
                savedStateHandle[VIDEO_DATA] = newData
                savedStateHandle[EPISODE_SELECTED] = newEpisode
            },
            updateSeason = { newSeason ->
                _season.update { Resource.Success(newSeason!!) }
            }
        )
    }

    fun onSeasonChange(seasonNumber: Int) {
        if (onSeasonChangeJob?.isActive == true)
            return

        onSeasonChangeJob = viewModelScope.launch {
            _season.update { Resource.Loading }
            _season.update {
                val result = fetchSeasonFromProvider(
                    showId = watchHistoryItem.value.id,
                    seasonNumber = seasonNumber
                )

                if (result == null)
                    Resource.Failure("Could not fetch season data")
                else {
                    Resource.Success(result)
                }
            }
        }
    }

    fun showSnackbar(message: String, type: PlayerSnackbarMessageType) {
        if (onShowSnackbarJob?.isActive == true)
            return

        onShowSnackbarJob = viewModelScope.launch {
            val itemIndexInQueue = snackbarQueue.indexOfFirst {
                it.type == type
            }
            val isSameTypeAlreadyQueued = itemIndexInQueue != -1

            if (!isSameTypeAlreadyQueued) {
                snackbarQueue.add(PlayerSnackbarMessage(message, type))
                return@launch
            }

            when (type) {
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
        if (onRemoveSnackbarJob?.isActive == true)
            return

        onRemoveSnackbarJob = viewModelScope.launch {
            if (position > snackbarQueue.size - 1)
                return@launch

            snackbarQueue.removeAt(position)
        }
    }
}