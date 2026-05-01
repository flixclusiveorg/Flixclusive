package com.flixclusive.domain.provider.usecase.get.impl

import com.flixclusive.core.common.domain.Async
import com.flixclusive.core.common.locale.UiText
import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.data.database.repository.WatchProgressRepository
import com.flixclusive.domain.provider.R
import com.flixclusive.domain.provider.model.EpisodeWithProgress
import com.flixclusive.domain.provider.model.SeasonWithProgress
import com.flixclusive.domain.provider.usecase.get.GetSeasonWithWatchProgressUseCase
import com.flixclusive.model.media.Show
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
        show: Show,
        number: Int,
    ): Flow<Async<SeasonWithProgress>> =
        channelFlow {
            trySend(Async.Loading)
            val season = show.getSeason(number)

            if (season == null) {
                trySend(Async.Failure(UiText.from(R.string.failed_to_fetch_season_message, number)))
                return@channelFlow
            }

            val userId = userSessionDataStore.currentUserId.filterNotNull().first()
            watchProgressRepository.getSeasonProgressAsFlow(
                tvShowId = show.id,
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

                send(Async.Success(SeasonWithProgress(season = season, episodes = episodes)))
            }
        }
}
