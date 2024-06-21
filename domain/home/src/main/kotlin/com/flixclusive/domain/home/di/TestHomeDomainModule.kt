package com.flixclusive.domain.home.di

import com.flixclusive.data.configuration.di.test.TestAppConfigurationModule.getMockAppConfigurationManager
import com.flixclusive.data.watch_history.di.TestWatchHistoryDataModule.getMockWatchHistoryRepository
import com.flixclusive.domain.category.di.TestCategoryDomainModule.getMockCategoryItemsProviderUseCase
import com.flixclusive.domain.home.HomeItemsProviderUseCase
import com.flixclusive.domain.provider.di.TestProviderDomainModule.getMockSourceLinksProviderUseCase
import com.flixclusive.domain.tmdb.test.TestTmdbDomainModule.getMockFilmProviderUseCase
import kotlinx.coroutines.CoroutineScope

object TestHomeDomainModule {
    fun getMockHomeItemsProviderUseCase(
        scope: CoroutineScope
    ): HomeItemsProviderUseCase {
        return HomeItemsProviderUseCase(
            scope = scope,
            filmProviderUseCase = getMockFilmProviderUseCase(),
            configurationProvider = getMockAppConfigurationManager(),
            watchHistoryRepository = getMockWatchHistoryRepository(),
            sourceLinksProvider = getMockSourceLinksProviderUseCase(),
            categoryItemsProviderUseCase = getMockCategoryItemsProviderUseCase()
        )
    }
}