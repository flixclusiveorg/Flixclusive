package com.flixclusive.data.configuration

import com.flixclusive.core.datastore.AppSettingsManager
import com.flixclusive.core.network.retrofit.GithubRawApiService
import com.flixclusive.core.network.util.Resource
import com.flixclusive.core.network.util.Resource.Failure.Companion.toNetworkException
import com.flixclusive.core.util.coroutines.AppDispatchers
import com.flixclusive.core.util.log.errorLog
import com.flixclusive.core.util.network.okhttp.UserAgentManager
import com.flixclusive.model.configuration.catalog.HomeCatalogsData
import com.flixclusive.model.configuration.catalog.SearchCatalogsData
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import javax.inject.Inject
import javax.inject.Singleton
import com.flixclusive.core.locale.R as LocaleR

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
    val commitVersion: String,
)

private const val MAX_RETRIES = 5

@Singleton
class AppConfigurationManager @Inject constructor(
    private val githubRawApiService: GithubRawApiService,
    private val appUpdateChecker: AppUpdateChecker,
    private val appSettingsManager: AppSettingsManager,
    client: OkHttpClient,
) {
    private val userAgentManager = UserAgentManager(client)

    private var fetchJob: Job? = null

    private val _configurationStatus = MutableStateFlow<Resource<Unit>>(Resource.Loading)
    val configurationStatus = _configurationStatus.asStateFlow()

    private val _updateStatus = MutableStateFlow<UpdateStatus>(UpdateStatus.Fetching)
    val updateStatus = _updateStatus.asStateFlow()

    var currentAppBuild: AppBuild? = null
        private set

    var appUpdateInfo: AppUpdateInfo? = null
    var homeCatalogsData: HomeCatalogsData? = null
    var searchCatalogsData: SearchCatalogsData? = null

    fun initialize(appBuild: AppBuild? = null) {
        if(fetchJob?.isActive == true)
            return

        if(currentAppBuild == null)
            currentAppBuild = appBuild

        fetchJob = AppDispatchers.IO.scope.launch {
            val retryDelay = 3000L
            for (i in 0..MAX_RETRIES) {
                _configurationStatus.update { Resource.Loading }

                try {
                    checkForUpdates()

                    if (UserAgentManager.desktopUserAgents.isEmpty()) {
                        userAgentManager.loadLatestUserAgents()
                    }

                    if (homeCatalogsData == null) {
                        homeCatalogsData = githubRawApiService.getHomeCatalogsConfig()
                    }

                    if (searchCatalogsData == null) {
                        searchCatalogsData = githubRawApiService.getSearchCatalogsConfig()
                    }

                    return@launch _configurationStatus.update { Resource.Success(Unit) }
                } catch (e: Exception) {
                    errorLog(e)

                    if (i == MAX_RETRIES) {
                        val errorMessageId = e.toNetworkException().error!!

                        return@launch _configurationStatus.update { Resource.Failure(errorMessageId) }
                    }
                }

                delay(retryDelay)
            }

            _configurationStatus.update {
                Resource.Failure(LocaleR.string.failed_to_init_app)
            }
        }
    }

    suspend fun checkForUpdates() {
        _updateStatus.update { UpdateStatus.Fetching }

        try {
            val appSettings = appSettingsManager.appSettings.data.first()
            val isUsingPrereleaseUpdates = appSettings.isUsingPrereleaseUpdates

            val status = if (isUsingPrereleaseUpdates && currentAppBuild?.debug == false) {
                appUpdateChecker.checkForPrereleaseUpdates(
                    currentAppBuild = currentAppBuild!!
                )
            } else {
                appUpdateChecker.checkForStableUpdates(
                    currentAppBuild = currentAppBuild!!
                )
            }

            if (status is UpdateStatus.Outdated || status is UpdateStatus.UpToDate) {
                appUpdateInfo = status.updateInfo
            }

            _updateStatus.update { status }
        } catch (e: Exception) {
            errorLog(e)
            val errorMessageId = e.toNetworkException().error!!

            _updateStatus.update { UpdateStatus.Error(errorMessageId) }
        }
    }
}