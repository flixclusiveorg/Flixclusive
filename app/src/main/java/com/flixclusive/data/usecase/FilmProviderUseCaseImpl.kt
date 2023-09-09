package com.flixclusive.data.usecase

import com.flixclusive.domain.common.Resource
import com.flixclusive.domain.model.tmdb.Film
import com.flixclusive.domain.model.tmdb.FilmType
import com.flixclusive.domain.repository.TMDBRepository
import com.flixclusive.domain.usecase.FilmProviderUseCase
import javax.inject.Inject

class FilmProviderUseCaseImpl @Inject constructor(
    private val tmdbRepository: TMDBRepository
) : FilmProviderUseCase {
    override suspend fun invoke(
        id: Int,
        type: FilmType,
        onError: () -> Unit,
        onSuccess: (Film?) -> Unit,
    ) {
        val result: Resource<Film> = when (type) {
            FilmType.MOVIE -> tmdbRepository.getMovie(id = id)
            FilmType.TV_SHOW -> tmdbRepository.getTvShow(id = id)
        }

        when (result) {
            is Resource.Failure -> onError()
            Resource.Loading -> Unit
            is Resource.Success -> onSuccess(result.data)
        }
    }
}