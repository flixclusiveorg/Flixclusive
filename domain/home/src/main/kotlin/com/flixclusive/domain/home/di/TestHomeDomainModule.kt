package com.flixclusive.domain.home.di

import com.flixclusive.data.configuration.di.test.TestAppConfigurationModule.getMockAppConfigurationManager
import com.flixclusive.data.provider.di.TestProviderDataModule.getMockProviderManager
import com.flixclusive.data.watch_history.di.TestWatchHistoryDataModule.getMockWatchHistoryRepository
import com.flixclusive.domain.catalog.di.TestCatalogDomainModule.getMockCatalogItemsProviderUseCase
import com.flixclusive.domain.home.HomeItemsProviderUseCase
import com.flixclusive.domain.tmdb.test.TestTmdbDomainModule.getMockFilmProviderUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope

object TestHomeDomainModule {
    fun getMockHomeItemsProviderUseCase(
        scope: CoroutineScope,
        dispatcher: CoroutineDispatcher,
    ): HomeItemsProviderUseCase {
        return HomeItemsProviderUseCase(
            filmProviderUseCase = getMockFilmProviderUseCase(dispatcher = dispatcher),
            watchHistoryRepository = getMockWatchHistoryRepository(),
            configurationProvider = getMockAppConfigurationManager(),
            catalogItemsProviderUseCase = getMockCatalogItemsProviderUseCase(),
            providerManager = getMockProviderManager()
        )
    }
}