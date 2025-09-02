package com.flixclusive.domain.provider.usecase.get.impl

import com.flixclusive.data.tmdb.repository.TMDBMetadataRepository
import com.flixclusive.domain.provider.usecase.get.GetEpisodeUseCase
import com.flixclusive.model.film.TvShow
import com.flixclusive.model.film.common.tv.Episode
import javax.inject.Inject

internal class GetEpisodeUseCaseImpl
    @Inject
    constructor(
        private val tmdbMetadataRepository: TMDBMetadataRepository,
    ) : GetEpisodeUseCase {
        override suspend operator fun invoke(
            tvShow: TvShow,
            season: Int,
            episode: Int,
        ): Episode? {
            var seasonData = tvShow.seasons.find { it.number == season }

            if (seasonData == null && tvShow.isFromTmdb && tvShow.tmdbId != null) {
                seasonData = tmdbMetadataRepository
                    .getSeason(
                        id = tvShow.tmdbId!!,
                        seasonNumber = season,
                    ).data
            }

            if (seasonData == null || seasonData.episodes.isEmpty()) return null

            if (seasonData.episodes.size < episode) {
                return invoke(tvShow = tvShow, season = season + 1, episode = 1)
            }

            return seasonData.episodes.find { it.number == episode }
        }
    }
