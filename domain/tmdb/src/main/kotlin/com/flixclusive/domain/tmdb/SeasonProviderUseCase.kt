package com.flixclusive.domain.tmdb

import com.flixclusive.core.network.util.Resource
import com.flixclusive.core.locale.UiText
import com.flixclusive.data.tmdb.TMDBRepository
import com.flixclusive.data.watch_history.WatchHistoryRepository
import com.flixclusive.model.film.TvShow
import com.flixclusive.model.film.common.tv.Season
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import com.flixclusive.core.locale.R as LocaleR

class SeasonProviderUseCase @Inject constructor(
    private val tmdbRepository: TMDBRepository,
    private val watchHistoryRepository: WatchHistoryRepository,
) {
    fun asFlow(tvShow: TvShow, seasonNumber: Int) = flow {
        emit(Resource.Loading)

        val noSeasonFoundError = Resource.Failure(UiText.StringResource(LocaleR.string.failed_to_fetch_season_message))

        if (!tvShow.isFromTmdb) {
            val season = tvShow.seasons
                .find { it.number == seasonNumber }

            if (season != null)
               return@flow emit(Resource.Success(season))

            return@flow emit(noSeasonFoundError)
        }

        when (
            val result = tmdbRepository.getSeason(
                id = tvShow.tmdbId!!,
                seasonNumber = seasonNumber
            )
        ) {
            is Resource.Failure -> emit(noSeasonFoundError)
            Resource.Loading -> Unit
            is Resource.Success -> {
                val watchHistoryItem = watchHistoryRepository.getWatchHistoryItemById(tvShow.identifier)
                watchHistoryItem?.let { item ->
                    result.data?.episodes?.size?.let {
                        val newEpisodesMap = item.episodes.toMutableMap()
                        newEpisodesMap[seasonNumber] = it

                        watchHistoryRepository.insert(item.copy(episodes = newEpisodesMap))
                    }
                }

                emit(result)
            }
        }
    }

    suspend operator fun invoke(tvShow: TvShow, seasonNumber: Int): Resource<Season> {
        val noSeasonFoundError = Resource.Failure(UiText.StringResource(LocaleR.string.failed_to_fetch_season_message))

        if (!tvShow.isFromTmdb) {
            val season = tvShow.seasons
                .find { it.number == seasonNumber }

            if (season != null)
                return Resource.Success(season)

            return noSeasonFoundError
        }

        return when (
            val result = tmdbRepository.getSeason(
                id = tvShow.tmdbId!!,
                seasonNumber = seasonNumber
            )
        ) {
            is Resource.Success -> {
                val watchHistoryItem = watchHistoryRepository.getWatchHistoryItemById(tvShow.identifier)
                watchHistoryItem?.let { item ->
                    result.data?.episodes?.size?.let {
                        val newEpisodesMap = item.episodes.toMutableMap()
                        newEpisodesMap[seasonNumber] = it

                        watchHistoryRepository.insert(item.copy(episodes = newEpisodesMap))
                    }
                }

                result
            }
            else -> noSeasonFoundError
        }
    }
}