package com.flixclusive.domain.config

import com.flixclusive.common.UiText
import com.flixclusive.domain.model.config.AppConfig
import com.flixclusive.domain.model.config.HomeCategoriesConfig
import com.flixclusive.domain.model.config.ProviderStatus
import com.flixclusive.domain.model.config.SearchCategoriesConfig
import kotlinx.coroutines.flow.StateFlow

sealed class RemoteConfigStatus(val errorMessage: UiText? = null) {
    data object Loading :  RemoteConfigStatus()
    data object Success :  RemoteConfigStatus()
    data class Error(
        val message: UiText
    ) : RemoteConfigStatus(message)
}

interface ConfigurationProvider {
    val remoteStatus: StateFlow<RemoteConfigStatus>

    var homeCategoriesConfig: HomeCategoriesConfig?
    var searchCategoriesConfig: SearchCategoriesConfig?
    var appConfig: AppConfig?
    var providersStatus: List<ProviderStatus>?

    fun initialize()
    fun checkForUpdates()
}