package com.flixclusive.presentation.tv.screens.player

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.media3.common.util.UnstableApi
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
import okhttp3.OkHttpClient
import javax.inject.Inject

@UnstableApi
@HiltViewModel
class TvPlayerViewModel @Inject constructor(
    @ApplicationContext context: Context,
    savedStateHandle: SavedStateHandle,
    sourceLinksProvider: SourceLinksProviderUseCase,
    appSettingsManager: AppSettingsManager,
    watchHistoryRepository: WatchHistoryRepository,
    val client: OkHttpClient,
    private val watchHistoryItemManager: WatchHistoryItemManagerUseCase,
    private val seasonProvider: SeasonProviderUseCase,
) : BasePlayerViewModel(
    context = context,
    watchHistoryRepository = watchHistoryRepository,
    savedStateHandle = savedStateHandle,
    appSettingsManager = appSettingsManager,
    sourceLinksProvider = sourceLinksProvider,
    watchHistoryItemManager = watchHistoryItemManager,
) {
    private var season: Season? = null

    fun play(episodeToPlay: TMDBEpisode? = null) {
        loadSourceData(episodeToWatch = episodeToPlay, updateSeason = { newSeason ->
            season = newSeason
        })
    }

    suspend fun initializeWatchItemManager(seasonNumber: Int) {
        fetchSeasonFromProvider(film.id, seasonNumber)
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
                showId = film.id,
                seasonNumber = seasonNumber
            )
        }

        return seasonToUse
    }
}
