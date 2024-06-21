package com.flixclusive.domain.category.di

import com.flixclusive.data.provider.di.TestProviderDataModule.getMockProviderApiRepository
import com.flixclusive.data.tmdb.di.TestTmdbDataModule.getMockTMDBRepository
import com.flixclusive.domain.category.CategoryItemsProviderUseCase

object TestCategoryDomainModule {
    fun getMockCategoryItemsProviderUseCase(): CategoryItemsProviderUseCase {
        return CategoryItemsProviderUseCase(
            tmdbRepository = getMockTMDBRepository(),
            providerApiRepository = getMockProviderApiRepository()
        )
    }
}