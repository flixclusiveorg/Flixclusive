package com.flixclusive.domain.provider.usecase.get.impl

import com.flixclusive.core.common.locale.UiText
import com.flixclusive.core.network.util.Resource
import com.flixclusive.data.tmdb.repository.TMDBMetadataRepository
import com.flixclusive.domain.provider.R
import com.flixclusive.domain.provider.usecase.get.GetSeasonUseCase
import com.flixclusive.domain.provider.util.extensions.isNonDefaultProvider
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

                val response = when {
                    tvShow.isNonDefaultProvider -> {
                        getFromNonDefaultProvider(tvShow, number)
                    }

                    tvShow.isFromTmdb && tvShow.tmdbId != null -> {
                        tmdbMetadataRepository.getSeason(
                            id = tvShow.tmdbId!!,
                            seasonNumber = number,
                        )
                    }

                    else -> {
                        Resource.Failure(
                            UiText.from(R.string.failed_to_fetch_season_message, number),
                        )
                    }
                }

                emit(response)
            }

        /**
         * Fetches the season from the TvShow object for non-default providers.
         *
         * @param tvShow The TvShow object containing the seasons.
         * @param number The season number to fetch.
         *
         * @return A [Resource] containing the [Season] if found, or with an error message
         * */
        private fun getFromNonDefaultProvider(
            tvShow: TvShow,
            number: Int,
        ): Resource<Season> {
            val season = tvShow.seasons.find { it.number == number }

            return if (season != null) {
                Resource.Success(season)
            } else {
                Resource.Failure(UiText.from(R.string.failed_to_fetch_season_message, number))
            }
        }
    }
