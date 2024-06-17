package com.flixclusive.domain.tmdb

import com.flixclusive.core.util.common.resource.Resource
import com.flixclusive.core.util.common.ui.UiText
import com.flixclusive.core.util.film.FilmType
import com.flixclusive.data.tmdb.TMDBRepository
import com.flixclusive.model.tmdb.Film
import com.flixclusive.model.tmdb.FilmDetails
import javax.inject.Inject
import com.flixclusive.core.util.R as UtilR

class FilmProviderUseCase @Inject constructor(
    private val tmdbRepository: TMDBRepository
) {
    suspend operator fun invoke(partiallyDetailedFilm: Film): Resource<FilmDetails> {
        return partiallyDetailedFilm.run {
            when {
                isMovie && isFromTmdb -> tmdbRepository.getMovie(id = tmdbId!!)
                isTvShow && isFromTmdb -> tmdbRepository.getTvShow(id = tmdbId!!)
                else -> Resource.Failure(UiText.StringResource(UtilR.string.film_not_found))
            }
        }
    }

    private val Film.isTvShow: Boolean
        get() = filmType == FilmType.TV_SHOW
    
    private val Film.isMovie: Boolean
        get() = filmType == FilmType.MOVIE
}