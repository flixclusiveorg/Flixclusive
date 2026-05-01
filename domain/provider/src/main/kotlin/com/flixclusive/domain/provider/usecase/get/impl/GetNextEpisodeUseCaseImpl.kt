package com.flixclusive.domain.provider.usecase.get.impl

import com.flixclusive.domain.provider.usecase.get.GetNextEpisodeUseCase
import com.flixclusive.model.media.Show
import com.flixclusive.model.media.common.tv.Episode
import javax.inject.Inject

internal class GetNextEpisodeUseCaseImpl @Inject constructor() : GetNextEpisodeUseCase {
    override suspend operator fun invoke(
        show: Show,
        season: Int,
        episode: Int,
    ): Episode? {
        val nextEpisode = episode + 1
        val seasonData = show.getSeason(season) ?: return null

        if (seasonData.episodes.isEmpty()) return null

        if (seasonData.episodes.size < nextEpisode) {
            return invoke(show = show, season = season + 1, episode = 0)
        }

        val episodeIndex =  seasonData.episodes.binarySearch {
            it.number.compareTo(nextEpisode)
        }

        return seasonData.episodes.getOrNull(episodeIndex)
    }
}
