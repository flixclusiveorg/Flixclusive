package com.flixclusive.domain.provider.usecase.get.impl

import android.content.Context
import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.core.util.exception.safeCall
import com.flixclusive.data.provider.repository.ProviderRepository
import com.flixclusive.domain.provider.usecase.get.GetNextEpisodeUseCase
import com.flixclusive.model.media.Show
import com.flixclusive.model.media.common.tv.Episode
import com.flixclusive.model.media.common.tv.Season
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import javax.inject.Inject

internal class GetNextEpisodeUseCaseImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val providerRepository: ProviderRepository,
    private val userSessionDataStore: UserSessionDataStore
) : GetNextEpisodeUseCase {
    override suspend operator fun invoke(
        show: Show,
        season: Int,
        episode: Int,
    ): Episode? {
        val nextEpisode = episode + 1
        var seasonData = show.getSeason(season) ?: return null

        if (seasonData.episodeCount == 0) return null
        if (seasonData is Season.Partial) {
            seasonData = getFullSeasonData(
                show = show,
                season = seasonData
            ) ?: return null
        }

        if ((seasonData as Season.Full).episodes.size < nextEpisode) {
            return invoke(show = show, season = season + 1, episode = 0)
        }

        val episodeIndex = seasonData.episodes.binarySearch {
            it.number.compareTo(nextEpisode)
        }

        return seasonData.episodes.getOrNull(episodeIndex)
    }

    private suspend fun getFullSeasonData(
        show: Show,
        season: Season.Partial
    ): Season.Full? {
        val userId = userSessionDataStore.currentUserId.filterNotNull().first()
        val provider = providerRepository.getProvider(
            id = show.providerId,
            ownerId = userId
        )

        val api = safeCall {
            provider?.plugin?.getMetadataApi(context)
        } ?: return null

        return safeCall {
            api.getSeason(show, season)
        }
    }
}
