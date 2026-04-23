package com.flixclusive.domain.provider.usecase.get.impl

import com.flixclusive.core.common.locale.UiText
import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.core.network.util.Resource
import com.flixclusive.data.database.repository.WatchProgressRepository
import com.flixclusive.domain.provider.R
import com.flixclusive.domain.provider.model.EpisodeWithProgress
import com.flixclusive.domain.provider.model.SeasonWithProgress
import com.flixclusive.domain.provider.usecase.get.GetSeasonWithWatchProgressUseCase
import com.flixclusive.model.film.TvShow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import javax.inject.Inject

internal class GetSeasonWithWatchProgressUseCaseImpl @Inject constructor(
    private val watchProgressRepository: WatchProgressRepository,
    private val userSessionDataStore: UserSessionDataStore
) : GetSeasonWithWatchProgressUseCase {
    override fun invoke(
        tvShow: TvShow,
        number: Int,
    ): Flow<Resource<SeasonWithProgress>> =
        channelFlow {
            trySend(Resource.Loading)

            // Try to get the season from the TvShow.seasons property first
            val seasonIndex = tvShow.seasons.binarySearch {
                it.number.compareTo(number)
            }

            val season = tvShow.seasons.getOrNull(seasonIndex)

            if (season == null) {
                trySend(Resource.Failure(UiText.from(R.string.failed_to_fetch_season_message, number)))
                return@channelFlow
            }

            val userId = userSessionDataStore.currentUserId.filterNotNull().first()
            watchProgressRepository.getSeasonProgressAsFlow(
                tvShowId = tvShow.id,
                seasonNumber = number,
                ownerId = userId,
            ).collect { list ->
                val episodes = season.episodes.map { episode ->
                    val episodeIndex = list.binarySearchBy(episode.number) { it.episodeNumber }

                    EpisodeWithProgress(
                        episode = episode,
                        watchProgress = list.getOrNull(episodeIndex),
                    )
                }

                send(Resource.Success(SeasonWithProgress(season = season, episodes = episodes)))
            }
        }
}
