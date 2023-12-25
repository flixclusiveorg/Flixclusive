package com.flixclusive.presentation.tv.screens.player

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.flixclusive.domain.model.entities.WatchHistoryItem
import com.flixclusive.domain.model.tmdb.Film
import com.flixclusive.domain.model.tmdb.Season
import com.flixclusive.domain.model.tmdb.TMDBEpisode
import com.flixclusive.domain.preferences.AppSettingsManager
import com.flixclusive.domain.repository.ProvidersRepository
import com.flixclusive.domain.repository.WatchHistoryRepository
import com.flixclusive.domain.usecase.SeasonProviderUseCase
import com.flixclusive.domain.usecase.VideoDataProviderUseCase
import com.flixclusive.domain.usecase.WatchHistoryItemManagerUseCase
import com.flixclusive.presentation.common.viewmodels.film.FilmScreenNavArgs
import com.flixclusive.presentation.common.viewmodels.player.BasePlayerViewModel
import com.flixclusive.presentation.navArgs
import com.flixclusive.providers.models.common.VideoData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import javax.inject.Inject

@UnstableApi
@HiltViewModel
class TvPlayerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    videoDataProvider: VideoDataProviderUseCase,
    appSettingsManager: AppSettingsManager,
    watchHistoryRepository: WatchHistoryRepository,
    providersRepository: ProvidersRepository,
    val client: OkHttpClient,
    private val watchHistoryItemManager: WatchHistoryItemManagerUseCase,
    private val seasonProvider: SeasonProviderUseCase,
) : BasePlayerViewModel(
    appSettingsManager = appSettingsManager,
    providersRepository = providersRepository,
    videoDataProvider = videoDataProvider
) {
    private val filmArgs = savedStateHandle.navArgs<FilmScreenNavArgs>()

    override val watchHistoryItem = watchHistoryRepository.getWatchHistoryItemByIdInFlow(itemId = filmArgs.film.id)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = WatchHistoryItem()
        )

    private var season: Season? = null
    private var seasonCount: Int? = null

    fun play(film: Film, episodeToPlay: TMDBEpisode? = null) {
        loadVideoData(
            film = film,
            seasonCount = seasonCount!!,
            episodeToWatch = episodeToPlay,
            updateSeason = { newSeason ->
                season = newSeason
            }
        )
    }

    override fun onSuccessCallback(newData: VideoData, newEpisode: TMDBEpisode?) {
        _videoData.update { newData }
        _currentSelectedEpisode.update { newEpisode }
    }

    override fun updateWatchHistory(currentTime: Long, duration: Long) {
        viewModelScope.launch {
            watchHistoryItemManager.updateWatchHistoryItem(
                watchHistoryItem = watchHistoryItem.value ?: WatchHistoryItem(),
                currentTime = currentTime,
                totalDuration = duration,
            )
        }
    }

    suspend fun initializeWatchItemManager(seasonNumber: Int) {
        val filmId = filmArgs.film.id
        fetchSeasonFromProvider(filmId, seasonNumber)
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

    override fun onErrorCallback(message: String?) {
        TODO("Not yet implemented")
    }

    override suspend fun TMDBEpisode.fetchSeasonIfNeeded(seasonNumber: Int): Season? {
        var seasonToUse = this@TvPlayerViewModel.season

        val currentLoadedSeasonNumber = seasonToUse?.seasonNumber
        if (currentLoadedSeasonNumber != seasonNumber) {
            seasonToUse = fetchSeasonFromProvider(
                showId = filmArgs.film.id,
                seasonNumber = seasonNumber
            )
        }

        return seasonToUse
    }
}
