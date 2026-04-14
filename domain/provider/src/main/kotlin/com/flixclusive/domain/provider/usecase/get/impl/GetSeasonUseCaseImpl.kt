package com.flixclusive.domain.provider.usecase.get.impl

import com.flixclusive.core.common.locale.UiText
import com.flixclusive.core.network.util.Resource
import com.flixclusive.data.tmdb.repository.TMDBMetadataRepository
import com.flixclusive.domain.provider.R
import com.flixclusive.domain.provider.usecase.get.GetSeasonUseCase
import com.flixclusive.domain.provider.util.extensions.isFromTmdbSource
import com.flixclusive.model.film.TvShow
import com.flixclusive.model.film.common.tv.Season
import javax.inject.Inject

internal class GetSeasonUseCaseImpl @Inject constructor(
    private val tmdbMetadataRepository: TMDBMetadataRepository
) : GetSeasonUseCase {
    override suspend operator fun invoke(
        tvShow: TvShow,
        number: Int,
    ): Resource<Season?> {
        return try {
            val seasonIndex = tvShow.seasons.binarySearch {
                it.number.compareTo(number)
            }
            var season = tvShow.seasons.getOrNull(seasonIndex)

            if ((season == null || season.episodes.isEmpty()) && tvShow.isFromTmdbSource) {
                val tmdbSeason = tmdbMetadataRepository.getSeason(
                    id = tvShow.tmdbId!!,
                    seasonNumber = number,
                )

                if (tmdbSeason is Resource.Success) {
                    season = tmdbSeason.data
                } else if (tmdbSeason is Resource.Failure) {
                    return tmdbSeason
                }
            }

            if (season == null) {
                return Resource.Failure(UiText.from(R.string.failed_to_fetch_season_message, number))
            }

            Resource.Success(season)
        } catch (_: Exception) {
            Resource.Failure(UiText.from(R.string.failed_to_fetch_season_message, number))
        }
    }
}
