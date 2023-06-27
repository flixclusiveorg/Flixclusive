package com.flixclusive.domain.usecase

import com.flixclusive.domain.model.tmdb.Season

interface SeasonProviderUseCase {
    suspend operator fun invoke(id: Int, seasonNumber: Int): Season?
}