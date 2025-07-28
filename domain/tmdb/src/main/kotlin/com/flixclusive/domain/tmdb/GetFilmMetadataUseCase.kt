package com.flixclusive.domain.tmdb

import com.flixclusive.core.locale.UiText
import com.flixclusive.core.network.util.Resource
import com.flixclusive.core.util.coroutines.AppDispatchers.Companion.withIOContext
import com.flixclusive.core.util.exception.actualMessage
import com.flixclusive.core.util.log.errorLog
import com.flixclusive.data.provider.ProviderApiRepository
import com.flixclusive.data.tmdb.TMDBRepository
import com.flixclusive.model.film.DEFAULT_FILM_SOURCE_NAME
import com.flixclusive.model.film.Film
import com.flixclusive.model.film.FilmMetadata
import com.flixclusive.model.film.util.FilmType
import javax.inject.Inject
import com.flixclusive.core.locale.R as LocaleR

class GetFilmMetadataUseCase
    @Inject
    constructor(
        private val tmdbRepository: TMDBRepository,
        private val providerApiRepository: ProviderApiRepository,
    ) {
        suspend operator fun invoke(partiallyDetailedFilm: Film): Resource<FilmMetadata> {
            return partiallyDetailedFilm.run {
                if (!providerId.equals(DEFAULT_FILM_SOURCE_NAME, true)) {
                    return withIOContext {
                        try {
                            val api = providerApiRepository.getApi(providerId)!!
                            val filmMetadata = api.getMetadata(film = this@run)

                            Resource.Success(filmMetadata)
                        } catch (e: Exception) {
                            errorLog(e)
                            Resource.Failure(
                                UiText.StringResource(LocaleR.string.failed_to_fetch_data_message, e.actualMessage),
                            )
                        }
                    }
                }

                when {
                    isMovie && isFromTmdb -> tmdbRepository.getMovie(id = tmdbId!!)
                    isTvShow && isFromTmdb -> tmdbRepository.getTvShow(id = tmdbId!!)
                    else -> Resource.Failure(UiText.StringResource(LocaleR.string.film_not_found))
                }
            }
        }

        private val Film.isTvShow: Boolean
            get() = filmType == FilmType.TV_SHOW

        private val Film.isMovie: Boolean
            get() = filmType == FilmType.MOVIE
    }
