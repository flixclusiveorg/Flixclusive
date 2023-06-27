package com.flixclusive.data.usecase

import com.flixclusive.domain.common.Resource
import com.flixclusive.domain.model.tmdb.Season
import com.flixclusive.domain.repository.TMDBRepository
import com.flixclusive.domain.usecase.SeasonProviderUseCase
import javax.inject.Inject

class SeasonProviderUseCaseImpl @Inject constructor(
    private val tmdbRepository: TMDBRepository
) : SeasonProviderUseCase {
    override suspend fun invoke(id: Int, seasonNumber: Int): Season? {
        return when(
            val result =
                tmdbRepository.getSeason(id = id, seasonNumber = seasonNumber)
        ) {
            is Resource.Failure -> null
            Resource.Loading -> throw IllegalStateException("$result is not a valid return type!")
            is Resource.Success -> result.data!!
        }
    }
}