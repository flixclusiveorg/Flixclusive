package com.flixclusive.domain.provider.usecase.get.impl

import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.common.locale.UiText
import com.flixclusive.core.network.util.Resource
import com.flixclusive.core.util.exception.actualMessage
import com.flixclusive.core.util.log.errorLog
import com.flixclusive.data.provider.repository.ProviderApiRepository
import com.flixclusive.data.tmdb.repository.TMDBMetadataRepository
import com.flixclusive.domain.provider.R
import com.flixclusive.domain.provider.usecase.get.GetFilmMetadataUseCase
import com.flixclusive.domain.provider.util.extensions.isNonDefaultProvider
import com.flixclusive.model.film.Film
import com.flixclusive.model.film.FilmMetadata
import com.flixclusive.model.film.util.FilmType
import kotlinx.coroutines.withContext
import javax.inject.Inject

internal class GetFilmMetadataUseCaseImpl
    @Inject
    constructor(
        private val tmdbMetadataRepository: TMDBMetadataRepository,
        private val providerApiRepository: ProviderApiRepository,
        private val appDispatchers: AppDispatchers
    ) : GetFilmMetadataUseCase {
        override suspend operator fun invoke(film: Film): Resource<FilmMetadata> {
            return when {
                 film.isNonDefaultProvider -> {
                    withContext(appDispatchers.io) {
                        try {
                            val api = providerApiRepository.getApi(film.providerId)!!
                            val filmMetadata = api.getMetadata(film)

                            Resource.Success(filmMetadata)
                        } catch (e: Exception) {
                            errorLog(e)
                            Resource.Failure(
                                UiText.from(R.string.failed_to_fetch_data_message, e.actualMessage),
                            )
                        }
                    }
                }
                film.isFromTmdb -> getFromTmdb(tmdbId = film.tmdbId, filmType = film.filmType)
                else -> Resource.Failure(UiText.from(R.string.film_not_found))
            }
        }

        /**
         * Fetches film metadata from TMDB based on the film type (movie or TV show).
         *
         * @param tmdbId The TMDB ID of the film.
         * @param filmType The type of the film (movie or TV show).
         *
         * @return A [Resource] containing the [FilmMetadata] if successful, or an error message if failed.
         * */
        private suspend fun getFromTmdb(
            tmdbId: Int?,
            filmType: FilmType
        ): Resource<FilmMetadata> {
            if (tmdbId == null) {
                return Resource.Failure(UiText.from(R.string.no_tmdb_id_found))
            }

            return when (filmType) {
                FilmType.MOVIE -> tmdbMetadataRepository.getMovie(id = tmdbId)
                FilmType.TV_SHOW ->tmdbMetadataRepository.getTvShow(id = tmdbId)
            }
        }
    }
