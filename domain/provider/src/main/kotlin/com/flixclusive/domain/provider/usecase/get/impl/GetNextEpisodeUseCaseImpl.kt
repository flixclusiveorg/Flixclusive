package com.flixclusive.domain.provider.usecase.get.impl

import com.flixclusive.data.tmdb.repository.TMDBMetadataRepository
import com.flixclusive.domain.provider.usecase.get.GetNextEpisodeUseCase
import com.flixclusive.model.film.TvShow
import com.flixclusive.model.film.common.tv.Episode
import javax.inject.Inject

internal class GetNextEpisodeUseCaseImpl @Inject constructor(
    private val tmdbMetadataRepository: TMDBMetadataRepository,
) : GetNextEpisodeUseCase {
    override suspend operator fun invoke(
        tvShow: TvShow,
        season: Int,
        episode: Int,
    ): Episode? {
        val nextEpisode = episode + 1
        val seasonIndex = tvShow.seasons.binarySearch {
            it.number.compareTo(season)
        }

        var seasonData = tvShow.seasons.getOrNull(seasonIndex)
        if ((seasonData == null || seasonData.episodes.isEmpty()) && tvShow.isFromTmdb && tvShow.tmdbId != null) {
            seasonData = tmdbMetadataRepository.getSeason(
                id = tvShow.tmdbId!!,
                seasonNumber = season,
            ).data
        }

        if (seasonData == null || seasonData.episodes.isEmpty()) return null

        if (seasonData.episodes.size < nextEpisode) {
            return invoke(tvShow = tvShow, season = season + 1, episode = 0)
        }

        val episodeIndex =  seasonData.episodes.binarySearch {
            it.number.compareTo(nextEpisode)
        }

        return seasonData.episodes.getOrNull(episodeIndex)
    }
}
