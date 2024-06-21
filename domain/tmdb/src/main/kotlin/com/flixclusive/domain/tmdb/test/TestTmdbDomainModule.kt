package com.flixclusive.domain.tmdb.test

import com.flixclusive.data.tmdb.di.TestTmdbDataModule.getMockTMDBRepository
import com.flixclusive.domain.tmdb.FilmProviderUseCase

object TestTmdbDomainModule {
    fun getMockFilmProviderUseCase(): FilmProviderUseCase {
        return FilmProviderUseCase(
            tmdbRepository = getMockTMDBRepository()
        )
    }
}