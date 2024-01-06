package com.flixclusive.domain.tmdb

import com.flixclusive.core.util.common.resource.Resource
import com.flixclusive.core.util.film.FilmType
import com.flixclusive.data.tmdb.TMDBRepository
import com.flixclusive.model.tmdb.Film
import javax.inject.Inject

class FilmProviderUseCase @Inject constructor(
    private val tmdbRepository: TMDBRepository
) {
    suspend operator fun invoke(
        id: Int,
        type: FilmType
    ): Resource<Film> {
        return when (type) {
            FilmType.MOVIE -> tmdbRepository.getMovie(id = id)
            FilmType.TV_SHOW -> tmdbRepository.getTvShow(id = id)
        }
    }
}