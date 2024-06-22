package com.flixclusive.domain.tmdb

import com.flixclusive.core.util.common.dispatcher.AppDispatchers
import com.flixclusive.core.util.common.dispatcher.Dispatcher
import com.flixclusive.core.util.common.resource.Resource
import com.flixclusive.core.util.common.ui.UiText
import com.flixclusive.core.util.exception.actualMessage
import com.flixclusive.core.util.film.FilmType
import com.flixclusive.core.util.log.errorLog
import com.flixclusive.data.provider.ProviderApiRepository
import com.flixclusive.data.tmdb.TMDBRepository
import com.flixclusive.model.tmdb.Film
import com.flixclusive.model.tmdb.FilmDetails
import com.flixclusive.provider.ProviderApi
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import com.flixclusive.core.util.R as UtilR

class FilmProviderUseCase @Inject constructor(
    private val tmdbRepository: TMDBRepository,
    private val providerApiRepository: ProviderApiRepository,
    @Dispatcher(AppDispatchers.IO) private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(partiallyDetailedFilm: Film): Resource<FilmDetails> {
        return partiallyDetailedFilm.run {
            when {
                isMovie && isFromTmdb -> tmdbRepository.getMovie(id = tmdbId!!)
                isTvShow && isFromTmdb -> tmdbRepository.getTvShow(id = tmdbId!!)
                providerName != null -> {
                    withContext(ioDispatcher) {
                        try {
                            val api = providerName!!.providerApi!!
                            val detailedFilm = api.getFilmDetails(film = this@run)

                            Resource.Success(detailedFilm)
                        } catch (e: Exception) {
                            errorLog(e)
                            Resource.Failure(UiText.StringResource(UtilR.string.failed_to_fetch_data_message, e.actualMessage))
                        }
                    }
                }
                else -> Resource.Failure(UiText.StringResource(UtilR.string.film_not_found))
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