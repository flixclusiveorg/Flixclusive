package com.flixclusive.presentation.mobile.screens.player

import android.content.Context
import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.flixclusive.domain.common.Resource
import com.flixclusive.domain.model.tmdb.Season
import com.flixclusive.domain.model.tmdb.TMDBEpisode
import com.flixclusive.domain.preferences.AppSettingsManager
import com.flixclusive.domain.repository.WatchHistoryRepository
import com.flixclusive.domain.usecase.SeasonProviderUseCase
import com.flixclusive.domain.usecase.SourceLinksProviderUseCase
import com.flixclusive.domain.usecase.WatchHistoryItemManagerUseCase
import com.flixclusive.presentation.common.viewmodels.player.BasePlayerViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@UnstableApi
@HiltViewModel
class PlayerViewModel @Inject constructor(
    @ApplicationContext context: Context,
    sourceLinksProvider: SourceLinksProviderUseCase,
    appSettingsManager: AppSettingsManager,
    watchHistoryRepository: WatchHistoryRepository,
    savedStateHandle: SavedStateHandle,
    private val seasonProvider: SeasonProviderUseCase,
    private val watchHistoryItemManager: WatchHistoryItemManagerUseCase,
) : BasePlayerViewModel(
    context = context,
    watchHistoryRepository = watchHistoryRepository,
    savedStateHandle = savedStateHandle,
    appSettingsManager = appSettingsManager,
    sourceLinksProvider = sourceLinksProvider,
    watchHistoryItemManager = watchHistoryItemManager,
) {
    val snackbarQueue = mutableStateListOf<PlayerSnackbarMessage>()

    private val _season = MutableStateFlow<Resource<Season>?>(null)
    val season = _season.asStateFlow()
    // =====================================

    private var onSeasonChangeJob: Job? = null
    private var snackbarJobs: MutableList<Job?> =
        MutableList(PlayerSnackbarMessageType.entries.size) { null }

    init {
        viewModelScope.launch {
            currentSelectedEpisode.collectLatest {
                if(it?.season != null) {
                    if (it.season != seasonCount) {
                        fetchSeasonFromProvider(
                            showId = film.id,
                            seasonNumber = seasonCount!!
                        )
                    }

                    onSeasonChange(it.season)
                }
            }
        }

        resetUiState()
    }

    override fun onErrorCallback(message: String?) {
        showSnackbar(
            message = message ?: "Unknown error occurred",
            type = PlayerSnackbarMessageType.Error
        )
    }


    override suspend fun TMDBEpisode.fetchSeasonIfNeeded(seasonNumber: Int): Season? {
        var seasonToUse = _season.value!!.data

        val currentLoadedSeasonNumber = seasonToUse?.seasonNumber
        if (currentLoadedSeasonNumber != seasonNumber) {
            seasonToUse = fetchSeasonFromProvider(
                showId = film.id,
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
                )
        }

        return seasonToUse
    }

    /**
     * Callback function triggered when an episode is clicked.
     *
     * @param episodeToWatch The next episode to be played, or null if not available.
     */
    fun onEpisodeClick(episodeToWatch: TMDBEpisode? = null) {
        loadSourceData(
            episodeToWatch = episodeToWatch,
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
                    showId = film.id,
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
        val itemIndexInQueue = snackbarQueue.indexOfFirst {
            it.type == type
        }
        val isSameTypeAlreadyQueued = itemIndexInQueue != -1

        if (!isSameTypeAlreadyQueued) {
            snackbarJobs[type.ordinal] = viewModelScope.launch {
                val data = PlayerSnackbarMessage(message, type)
                snackbarQueue.add(data)

                if (type == PlayerSnackbarMessageType.Episode)
                    return@launch

                val durationInLong = when (data.duration) {
                    SnackbarDuration.Short -> 4000L
                    SnackbarDuration.Long -> 10000L
                    SnackbarDuration.Indefinite -> Long.MAX_VALUE
                }

                delay(durationInLong)
                snackbarQueue.remove(data)
            }
            return
        }

        when (type) {
            PlayerSnackbarMessageType.Episode -> {
                if (snackbarJobs[type.ordinal]?.isActive == true) {
                    snackbarJobs[type.ordinal]?.cancel()
                    snackbarJobs[type.ordinal] = null
                }

                snackbarJobs[type.ordinal] = viewModelScope.launch {
                    val currentIndex =
                        snackbarQueue.indexOfFirst { it.type == type } // Have to call this everytime to be cautious of other snackbar item changes :<

                    val itemInQueue = snackbarQueue[currentIndex]
                    snackbarQueue[currentIndex] = itemInQueue.copy(
                        message = message
                    )

                    delay(5000L)
                    snackbarQueue.remove(snackbarQueue[currentIndex])
                }
            }

            else -> {
                if (snackbarJobs[type.ordinal]?.isActive == true) {
                    snackbarJobs[type.ordinal]?.cancel()
                    snackbarJobs[type.ordinal] = null
                }

                snackbarQueue.remove(snackbarQueue[itemIndexInQueue])
                showSnackbar(message, type)
            }
        }
    }

    fun removeSnackbar(data: PlayerSnackbarMessage) {
        snackbarQueue.remove(data)
    }
}