package com.flixclusive.domain.tmdb.test

import com.flixclusive.data.provider.di.TestProviderDataModule.getMockProviderApiRepository
import com.flixclusive.data.tmdb.di.TestTmdbDataModule.getMockTMDBRepository
import com.flixclusive.domain.tmdb.FilmProviderUseCase
import kotlinx.coroutines.CoroutineDispatcher

object TestTmdbDomainModule {
    fun getMockFilmProviderUseCase(
        dispatcher: CoroutineDispatcher
    ): FilmProviderUseCase {
        return FilmProviderUseCase(
            tmdbRepository = getMockTMDBRepository(),
            providerApiRepository = getMockProviderApiRepository()
        )
    }
}