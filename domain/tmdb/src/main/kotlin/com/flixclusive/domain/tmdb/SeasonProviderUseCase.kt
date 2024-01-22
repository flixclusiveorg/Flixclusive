package com.flixclusive.domain.tmdb

import com.flixclusive.core.util.common.resource.Resource
import com.flixclusive.core.util.common.ui.UiText
import com.flixclusive.data.tmdb.TMDBRepository
import com.flixclusive.data.watch_history.WatchHistoryRepository
import com.flixclusive.model.tmdb.Season
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import com.flixclusive.core.util.R as UtilR

class SeasonProviderUseCase @Inject constructor(
    private val tmdbRepository: TMDBRepository,
    private val watchHistoryRepository: WatchHistoryRepository,
)  {
     fun asFlow(id: Int, seasonNumber: Int) = flow {
        emit(Resource.Loading)

        when(val result = tmdbRepository.getSeason(id = id, seasonNumber = seasonNumber)) {
            is Resource.Failure -> emit(
                Resource.Failure(
                    UiText.StringResource(UtilR.string.failed_to_fetch_season_message)
                )
            )
            Resource.Loading -> Unit
            is Resource.Success -> {
                val watchHistoryItem = watchHistoryRepository.getWatchHistoryItemById(id)

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

    suspend operator fun invoke(id: Int, seasonNumber: Int): Resource<Season> {
        return when(val result = tmdbRepository.getSeason(id = id, seasonNumber = seasonNumber)) {
            is Resource.Failure -> Resource.Failure(
                UiText.StringResource(UtilR.string.failed_to_fetch_season_message)
            )
            is Resource.Success -> {
                val watchHistoryItem = watchHistoryRepository.getWatchHistoryItemById(id)

                watchHistoryItem?.let { item ->
                    result.data?.episodes?.size?.let {
                        val newEpisodesMap = item.episodes.toMutableMap()
                        newEpisodesMap[seasonNumber] = it

                        watchHistoryRepository.insert(item.copy(episodes = newEpisodesMap))
                    }
                }

                result
            }
            else -> result
        }
    }
}