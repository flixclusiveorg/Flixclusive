package com.flixclusive.data.configuration

import com.flixclusive.core.util.common.ui.UiText
import com.flixclusive.model.configuration.AppConfig
import com.flixclusive.model.configuration.HomeCategoriesConfig
import com.flixclusive.model.configuration.ProviderStatus
import com.flixclusive.model.configuration.SearchCategoriesConfig
import kotlinx.coroutines.flow.StateFlow

sealed class RemoteConfigStatus(val errorMessage: UiText? = null) {
    data object Loading :  RemoteConfigStatus()
    data object Success :  RemoteConfigStatus()
    data class Error(
        val message: UiText
    ) : RemoteConfigStatus(message)
}

interface AppConfigurationManager {
    val remoteStatus: StateFlow<RemoteConfigStatus>

    var homeCategoriesConfig: HomeCategoriesConfig?
    var searchCategoriesConfig: SearchCategoriesConfig?
    var appConfig: AppConfig?
    var providersStatus: List<ProviderStatus>?

    fun initialize()
    fun checkForUpdates()
}