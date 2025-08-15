package com.flixclusive.domain.tmdb.usecase

import com.flixclusive.core.common.locale.UiText
import com.flixclusive.core.network.util.Resource
import com.flixclusive.core.util.coroutines.AppDispatchers.Companion.withIOContext
import com.flixclusive.core.util.exception.actualMessage
import com.flixclusive.core.util.log.errorLog
import com.flixclusive.data.provider.repository.ProviderApiRepository
import com.flixclusive.data.tmdb.repository.TMDBRepository
import com.flixclusive.domain.tmdb.R
import com.flixclusive.model.film.DEFAULT_FILM_SOURCE_NAME
import com.flixclusive.model.film.Film
import com.flixclusive.model.film.FilmMetadata
import com.flixclusive.model.film.util.FilmType
import javax.inject.Inject

class GetFilmMetadataUseCase
    @Inject
    constructor(
        private val tmdbRepository: TMDBRepository,
        private val providerApiRepository: ProviderApiRepository,
    ) {
        suspend operator fun invoke(partiallyDetailedFilm: Film): Resource<FilmMetadata> {
            return with(partiallyDetailedFilm) {
                if (!providerId.equals(DEFAULT_FILM_SOURCE_NAME, true)) {
                    return withIOContext {
                        try {
                            val api = providerApiRepository.getApi(providerId)!!
                            val filmMetadata = api.getMetadata(film = this@with)

                            Resource.Success(filmMetadata)
                        } catch (e: Exception) {
                            errorLog(e)
                            Resource.Failure(
                                UiText.from(R.string.failed_to_fetch_data_message, e.actualMessage),
                            )
                        }
                    }
                }

                when {
                    isMovie && isFromTmdb -> tmdbRepository.getMovie(id = tmdbId!!)
                    isTvShow && isFromTmdb -> tmdbRepository.getTvShow(id = tmdbId!!)
                    else -> Resource.Failure(UiText.from(R.string.film_not_found))
                }
            }
        }

        private val Film.isTvShow: Boolean
            get() = filmType == FilmType.TV_SHOW

        private val Film.isMovie: Boolean
            get() = filmType == FilmType.MOVIE
    }
