package com.flixclusive.data.configuration.di.test

import com.flixclusive.core.util.network.json.fromJson
import com.flixclusive.data.configuration.AppConfigurationManager
import com.flixclusive.data.configuration.AppUpdateInfo
import com.flixclusive.data.configuration.di.test.constant.APP_CONFIG
import com.flixclusive.data.configuration.di.test.constant.HOME_CATEGORIES
import com.flixclusive.data.configuration.di.test.constant.SEARCH_CATEGORIES
import com.flixclusive.model.configuration.catalog.HomeCatalogsData
import com.flixclusive.model.configuration.catalog.SearchCatalogsData
import io.mockk.every
import io.mockk.mockk

/**
 *
 * Mocks for AppConfigurationManager
 * */
object TestAppConfigurationModule {
    fun getMockAppConfigurationManager(): AppConfigurationManager {
        val homeCatalogsDataMock = fromJson<HomeCatalogsData>(HOME_CATEGORIES)
        val searchCatalogsDataMock = fromJson<SearchCatalogsData>(SEARCH_CATEGORIES)
        val appUpdateInfoMock = fromJson<AppUpdateInfo>(APP_CONFIG)

        val mock = mockk<AppConfigurationManager> {
            every { homeCatalogsData } returns  homeCatalogsDataMock
            every { searchCatalogsData } returns searchCatalogsDataMock
            every { appUpdateInfo } returns appUpdateInfoMock
        }

        return mock
    }
}