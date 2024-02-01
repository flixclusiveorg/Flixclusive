package com.flixclusive.data.configuration

import com.flixclusive.core.util.common.resource.Resource
import com.flixclusive.core.util.common.ui.UiText
import com.flixclusive.model.configuration.AppConfig
import com.flixclusive.model.configuration.HomeCategoriesConfig
import com.flixclusive.model.configuration.SearchCategoriesConfig
import kotlinx.coroutines.flow.SharedFlow

sealed class UpdateStatus(
    val errorMessage: UiText? = null
) {
    data object Fetching: UpdateStatus()
    data object Maintenance : UpdateStatus()
    data object Outdated : UpdateStatus()
    data object UpToDate : UpdateStatus()
    class Error(errorMessage: UiText?) : UpdateStatus(errorMessage)
}

/**
 *
 * Substitute model for BuildConfig
 * */
data class AppBuild(
    val applicationName: String,
    val applicationId: String,
    val debug: Boolean,
    val versionName: String,
    val build: Long,
)

interface AppConfigurationManager {
    val configurationStatus: SharedFlow<Resource<Unit>>
    val updateStatus: SharedFlow<UpdateStatus>

    /**
     *
     * Current [BuildConfig] of this app
     *
     * */
    val currentAppBuild: AppBuild?

    var homeCategoriesConfig: HomeCategoriesConfig?
    var searchCategoriesConfig: SearchCategoriesConfig?
    var appConfig: AppConfig?

    fun initialize(appBuild: AppBuild? = null)
    suspend fun checkForUpdates()
}