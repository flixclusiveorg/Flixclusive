package com.flixclusive.data.configuration

import com.flixclusive.core.datastore.AppSettingsManager
import com.flixclusive.core.network.retrofit.GithubApiService
import com.flixclusive.core.network.retrofit.GithubRawApiService
import com.flixclusive.core.util.common.configuration.GITHUB_REPOSITORY
import com.flixclusive.core.util.common.configuration.GITHUB_USERNAME
import com.flixclusive.core.util.common.dispatcher.di.ApplicationScope
import com.flixclusive.core.util.common.resource.Resource
import com.flixclusive.core.util.common.ui.UiText
import com.flixclusive.core.util.exception.catchInternetRelatedException
import com.flixclusive.core.util.log.errorLog
import com.flixclusive.model.configuration.AppConfig
import com.flixclusive.model.configuration.HomeCategoriesConfig
import com.flixclusive.model.configuration.SearchCategoriesConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.flixclusive.core.util.R as UtilR

private const val MAX_RETRIES = 5

internal class DefaultAppConfigurationManager @Inject constructor(
    private val githubRawApiService: GithubRawApiService,
    private val githubApiService: GithubApiService,
    private val appSettingsManager: AppSettingsManager,
    @ApplicationScope private val scope: CoroutineScope,
) : AppConfigurationManager {

    private var fetchJob: Job? = null

    private val _configurationStatus = MutableSharedFlow<Resource<Unit>>(replay = 1)
    override val configurationStatus = _configurationStatus.asSharedFlow()

    private val _updateStatus = MutableSharedFlow<UpdateStatus>(replay = 1)
    override val updateStatus = _updateStatus.asSharedFlow()

    override var currentAppBuild: AppBuild? = null
        private set

    override var appConfig: AppConfig? = null
    override var homeCategoriesConfig: HomeCategoriesConfig? = null
    override var searchCategoriesConfig: SearchCategoriesConfig? = null

    override fun initialize(appBuild: AppBuild?) {
        if(fetchJob?.isActive == true)
            return

        if(this.currentAppBuild == null)
            this.currentAppBuild = appBuild

        fetchJob = scope.launch {
            val retryDelay = 3000L
            for (i in 0..MAX_RETRIES) {
                _configurationStatus.emit(Resource.Loading)

                try {
                    checkForUpdates()

                    homeCategoriesConfig = githubRawApiService.getHomeCategoriesConfig()
                    searchCategoriesConfig = githubRawApiService.getSearchCategoriesConfig()

                    return@launch _configurationStatus.emit(Resource.Success(Unit))
                } catch (e: Exception) {
                    errorLog(e.stackTraceToString())

                    if (i == MAX_RETRIES) {
                        val errorMessageId = e.catchInternetRelatedException().error!!

                        return@launch _configurationStatus.emit(Resource.Failure(errorMessageId))
                    }
                }

                delay(retryDelay)
            }

            _configurationStatus.emit(
                Resource.Failure(UtilR.string.failed_to_init_app)
            )
        }
    }

    override suspend fun checkForUpdates() {
        _updateStatus.emit(UpdateStatus.Fetching)

        try {
            val isUsingPrereleaseUpdates = appSettingsManager.localAppSettings.isUsingPrereleaseUpdates

            appConfig = githubRawApiService.getAppConfig()

            if(appConfig!!.isMaintenance)
                return _updateStatus.emit(UpdateStatus.Maintenance)

            // TODO: REMOVE CONDITIONAL COMMENT BELOW
            if (isUsingPrereleaseUpdates /*&& currentAppBuild?.debug == false*/) {
                val lastCommitObject = githubApiService.getLastCommitObject()
                val appCommitVersion = currentAppBuild?.commitVersion
                    ?: throw NullPointerException("appCommitVersion should not be null!")


                val shortenedSha = lastCommitObject.lastCommit.sha.shortenSha()
                val isNeedingAnUpdate = appCommitVersion != shortenedSha

                if (isNeedingAnUpdate) {
                    val preReleaseInfo = githubApiService.getReleaseNotes(tag = "pre-release")

                    appConfig = appConfig!!.copy(
                        versionName = "PR-$shortenedSha",
                        updateInfo = preReleaseInfo.releaseNotes,
                        updateUrl = "https://github.com/$GITHUB_USERNAME/$GITHUB_REPOSITORY/releases/download/pre-release/flixclusive-release.apk"
                    )

                    return _updateStatus.emit(UpdateStatus.Outdated)
                }
            } else {
                val isNeedingAnUpdate = appConfig!!.build != -1L && appConfig!!.build > currentAppBuild!!.build

                if(isNeedingAnUpdate) {
                    val releaseInfo = githubApiService.getReleaseNotes(tag = appConfig!!.versionName)

                    appConfig = appConfig!!.copy(updateInfo = releaseInfo.releaseNotes)
                    return _updateStatus.emit(UpdateStatus.Outdated)
                }

                return _updateStatus.emit(UpdateStatus.UpToDate)
            }

            return _updateStatus.emit(UpdateStatus.Error(UiText.StringResource(UtilR.string.failed_checking_for_updates)))
        } catch (e: Exception) {
            errorLog(e.stackTraceToString())
            val errorMessageId = e.catchInternetRelatedException().error!!

            _updateStatus.emit(UpdateStatus.Error(errorMessageId))
        }
    }

    private fun String.shortenSha()
        = substring(0, 7)
}