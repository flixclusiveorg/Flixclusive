package com.flixclusive.domain.category.di

import com.flixclusive.core.util.common.dispatcher.AppDispatchers
import com.flixclusive.data.provider.di.TestProviderDataModule.getMockProviderApiRepository
import com.flixclusive.data.tmdb.di.TestTmdbDataModule.getMockTMDBRepository
import com.flixclusive.domain.category.CategoryItemsProviderUseCase
import kotlinx.coroutines.Dispatchers

object TestCategoryDomainModule {
    fun getMockCategoryItemsProviderUseCase(): CategoryItemsProviderUseCase {
        return CategoryItemsProviderUseCase(
            tmdbRepository = getMockTMDBRepository(),
            providerApiRepository = getMockProviderApiRepository(),
            ioDispatcher = AppDispatchers.IO.dispatcher,
        )
    }
}