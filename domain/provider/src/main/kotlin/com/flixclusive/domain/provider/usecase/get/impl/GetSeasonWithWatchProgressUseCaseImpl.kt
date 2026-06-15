package com.flixclusive.domain.provider.usecase.get.impl

import android.content.Context
import com.flixclusive.core.common.domain.Async
import com.flixclusive.core.common.locale.UiText
import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.core.util.log.errorLog
import com.flixclusive.data.database.repository.WatchProgressRepository
import com.flixclusive.data.provider.repository.ProviderRepository
import com.flixclusive.domain.provider.R
import com.flixclusive.domain.provider.model.EpisodeWithProgress
import com.flixclusive.domain.provider.model.SeasonWithProgress
import com.flixclusive.domain.provider.usecase.get.GetSeasonWithWatchProgressUseCase
import com.flixclusive.model.media.Show
import com.flixclusive.model.media.common.tv.Season
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import javax.inject.Inject

internal class GetSeasonWithWatchProgressUseCaseImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val watchProgressRepository: WatchProgressRepository,
    private val userSessionDataStore: UserSessionDataStore,
    private val providerRepository: ProviderRepository
) : GetSeasonWithWatchProgressUseCase {
    override fun invoke(
        show: Show,
        number: Int,
    ): Flow<Async<SeasonWithProgress>> =
        channelFlow {
            trySend(Async.Loading)
            var season = show.getSeason(number)
            val userId = userSessionDataStore.currentUserId.filterNotNull().first()

            if (season is Season.Partial) {
                try {
                    season = getFullSeasonData(
                        userId = userId,
                        show = show,
                        season = season
                    )
                } catch (_: UnsupportedOperationException) {
                    trySend(Async.Failure(UiText.from(R.string.error_provider_no_get_season_method)))
                    return@channelFlow
                } catch (e: Throwable) {
                    errorLog(e)
                    trySend(Async.Failure(UiText.from(R.string.failed_to_fetch_season_message, number)))
                    return@channelFlow
                }
            }

            if (season == null) {
                trySend(Async.Failure(UiText.from(R.string.failed_to_fetch_season_message, number)))
                return@channelFlow
            }

            watchProgressRepository
                .getSeasonProgressAsFlow(
                    tvShowId = show.id,
                    seasonNumber = number,
                    ownerId = userId,
                ).collectLatest { list ->
                    val episodes = (season as Season.Full).episodes.map { episode ->
                        val episodeIndex = list.binarySearchBy(episode.number) { it.episodeNumber }

                        EpisodeWithProgress(
                            episode = episode,
                            watchProgress = list.getOrNull(episodeIndex),
                        )
                    }

                    send(Async.Success(SeasonWithProgress(season = season, episodes = episodes)))
                }
        }

    private suspend fun getFullSeasonData(
        userId: String,
        show: Show,
        season: Season.Partial
    ): Season.Full? {
        val provider = providerRepository.getProvider(
            id = show.providerId,
            ownerId = userId
        )

        val api = provider?.plugin?.getMetadataApi(context)
            ?: throw UnsupportedOperationException("Provider does not support metadata API")

        return api.getSeason(show, season)
    }
}
