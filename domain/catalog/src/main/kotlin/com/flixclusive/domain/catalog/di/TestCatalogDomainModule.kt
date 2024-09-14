package com.flixclusive.domain.catalog.di

import com.flixclusive.data.provider.di.TestProviderDataModule.getMockProviderApiRepository
import com.flixclusive.data.tmdb.di.TestTmdbDataModule.getMockTMDBRepository
import com.flixclusive.domain.catalog.CatalogItemsProviderUseCase

object TestCatalogDomainModule {
    fun getMockCatalogItemsProviderUseCase(): CatalogItemsProviderUseCase {
        return CatalogItemsProviderUseCase(
            tmdbRepository = getMockTMDBRepository(),
            providerApiRepository = getMockProviderApiRepository()
        )
    }
}