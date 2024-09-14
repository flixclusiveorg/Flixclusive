package com.flixclusive.domain.tmdb

import com.flixclusive.core.util.coroutines.AppDispatchers.Companion.withIOContext
import com.flixclusive.core.network.util.Resource
import com.flixclusive.core.locale.UiText
import com.flixclusive.core.util.exception.actualMessage
import com.flixclusive.model.film.util.FilmType
import com.flixclusive.core.util.log.errorLog
import com.flixclusive.data.provider.ProviderApiRepository
import com.flixclusive.data.tmdb.TMDBRepository
import com.flixclusive.model.film.Film
import com.flixclusive.model.film.FilmDetails
import com.flixclusive.provider.ProviderApi
import javax.inject.Inject
import com.flixclusive.core.locale.R as LocaleR

class FilmProviderUseCase @Inject constructor(
    private val tmdbRepository: TMDBRepository,
    private val providerApiRepository: ProviderApiRepository
) {
    suspend operator fun invoke(partiallyDetailedFilm: Film): Resource<FilmDetails> {
        return partiallyDetailedFilm.run {
            when {
                isMovie && isFromTmdb -> tmdbRepository.getMovie(id = tmdbId!!)
                isTvShow && isFromTmdb -> tmdbRepository.getTvShow(id = tmdbId!!)
                providerName != null -> {
                    withIOContext {
                        try {
                            val api = providerName!!.providerApi!!
                            val detailedFilm = api.getFilmDetails(film = this@run)

                            Resource.Success(detailedFilm)
                        } catch (e: Exception) {
                            errorLog(e)
                            Resource.Failure(UiText.StringResource(LocaleR.string.failed_to_fetch_data_message, e.actualMessage))
                        }
                    }
                }
                else -> Resource.Failure(UiText.StringResource(LocaleR.string.film_not_found))
            }
        }
    }

    private val Film.isTvShow: Boolean
        get() = filmType == FilmType.TV_SHOW
    
    private val Film.isMovie: Boolean
        get() = filmType == FilmType.MOVIE

    private val String.providerApi: ProviderApi?
        get() = providerApiRepository.apiMap[this]
}