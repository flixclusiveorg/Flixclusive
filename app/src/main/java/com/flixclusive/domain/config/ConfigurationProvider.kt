package com.flixclusive.domain.config

import com.flixclusive.domain.model.config.AppConfig
import com.flixclusive.domain.model.config.HomeCategoriesConfig
import com.flixclusive.domain.model.config.SearchCategoriesConfig
import kotlinx.coroutines.flow.StateFlow

enum class RemoteConfigStatus {
    LOADING,
    SUCCESS,
    ERROR
}

interface ConfigurationProvider {
    val remoteStatus: StateFlow<RemoteConfigStatus>

    var homeCategoriesConfig: HomeCategoriesConfig?
    var searchCategoriesConfig: SearchCategoriesConfig?
    var appConfig: AppConfig?

    fun initialize()
}