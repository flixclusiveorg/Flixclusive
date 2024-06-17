package com.flixclusive.data.configuration.di.test

import com.flixclusive.core.util.network.fromJson
import com.flixclusive.data.configuration.AppConfigurationManager
import com.flixclusive.data.configuration.di.test.constant.APP_CONFIG
import com.flixclusive.data.configuration.di.test.constant.HOME_CATEGORIES
import com.flixclusive.data.configuration.di.test.constant.SEARCH_CATEGORIES
import com.flixclusive.model.configuration.AppConfig
import com.flixclusive.model.tmdb.category.HomeCategoriesData
import com.flixclusive.model.tmdb.category.SearchCategoriesData
import io.mockk.every
import io.mockk.mockk

/**
 *
 * Mocks for AppConfigurationManager
 * */
object TestAppConfigurationModule {
    fun getMockAppConfigurationManager(): AppConfigurationManager {
        val homeCategoriesDataMock = fromJson<HomeCategoriesData>(HOME_CATEGORIES)
        val searchCategoriesDataMock = fromJson<SearchCategoriesData>(SEARCH_CATEGORIES)
        val appConfigMock = fromJson<AppConfig>(APP_CONFIG)

        val mock = mockk<AppConfigurationManager> {
            every { homeCategoriesData } returns  homeCategoriesDataMock
            every { searchCategoriesData } returns searchCategoriesDataMock
            every { appConfig } returns appConfigMock
        }

        return mock
    }
}