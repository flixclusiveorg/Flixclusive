package com.flixclusive.domain.provider.usecase.get.impl

import com.flixclusive.core.common.locale.UiText
import com.flixclusive.core.network.util.Resource
import com.flixclusive.data.tmdb.repository.TMDBMetadataRepository
import com.flixclusive.domain.provider.R
import com.flixclusive.domain.provider.usecase.get.GetSeasonUseCase
import com.flixclusive.model.film.DEFAULT_FILM_SOURCE_NAME
import com.flixclusive.model.film.TvShow
import com.flixclusive.model.film.common.tv.Season
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

internal class GetSeasonUseCaseImpl
    @Inject
    constructor(
        private val tmdbMetadataRepository: TMDBMetadataRepository,
    ) : GetSeasonUseCase {
        override fun invoke(
            tvShow: TvShow,
            number: Int,
        ): Flow<Resource<Season>> =
            flow {
                emit(Resource.Loading)

                // Try to get the season from the TvShow.seasons property first
                var season = tvShow.seasons.find { it.number == number }

                if (season == null && tvShow.isFromTmdbSource) {
                    val tmdbSeason = tmdbMetadataRepository.getSeason(
                        id = tvShow.tmdbId!!,
                        seasonNumber = number,
                    )

                    return@flow emit(tmdbSeason)
                } else if (season != null) {
                    return@flow emit(Resource.Success(season))
                }

                emit(Resource.Failure(UiText.from(R.string.failed_to_fetch_season_message, number),))
            }
    }

/**
 * Checks if the TvShow is from TMDB source by verifying if it has a non-null tmdbId
 * and if the providerId matches the default film source name (case-insensitive).
 *
 * TODO: Update core-stubs's [TvShow.isFromTmdb] so no need to redefine this here.
 * */
private val TvShow.isFromTmdbSource: Boolean
    get() = tmdbId != null && providerId.equals(DEFAULT_FILM_SOURCE_NAME, ignoreCase = true)
