package com.flixclusive.domain.tmdb

import com.flixclusive.core.util.common.resource.Resource
import com.flixclusive.data.tmdb.TMDBRepository
import com.flixclusive.model.tmdb.Season
import javax.inject.Inject

class SeasonProviderUseCase @Inject constructor(
    private val tmdbRepository: TMDBRepository
)  {
    suspend fun invoke(id: Int, seasonNumber: Int): Season? {
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