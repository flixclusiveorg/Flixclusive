package com.flixclusive.data.configuration

import com.flixclusive.core.datastore.AppSettingsManager
import com.flixclusive.core.network.retrofit.GithubApiService
import com.flixclusive.core.network.retrofit.GithubRawApiService
import com.flixclusive.core.util.common.configuration.GITHUB_REPOSITORY
import com.flixclusive.core.util.common.configuration.GITHUB_USERNAME
import com.flixclusive.core.util.common.dispatcher.di.ApplicationScope
import com.flixclusive.core.util.common.resource.Resource
import com.flixclusive.core.util.exception.catchInternetRelatedException
import com.flixclusive.core.util.log.errorLog
import com.flixclusive.model.configuration.AppConfig
import com.flixclusive.model.tmdb.category.HomeCategoriesData
import com.flixclusive.model.tmdb.category.SearchCategoriesData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
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

    private val _configurationStatus = MutableStateFlow<Resource<Unit>>(Resource.Loading)
    override val configurationStatus = _configurationStatus.asStateFlow()

    private val _updateStatus = MutableStateFlow<UpdateStatus>(UpdateStatus.Fetching)
    override val updateStatus = _updateStatus.asStateFlow()

    override var currentAppBuild: AppBuild? = null
        private set

    override var appConfig: AppConfig? = null
    override var homeCategoriesData: HomeCategoriesData? = null
    override var searchCategoriesData: SearchCategoriesData? = null

    private val Resource<Unit>.needsToInitialize: Boolean
        get() = (this is Resource.Success
            && (appConfig == null || homeCategoriesData == null || searchCategoriesData == null))

    init {
        scope.launch {
            _configurationStatus.collectLatest {
                if(it.needsToInitialize)
                    initialize(currentAppBuild)
            }
        }
    }

    override fun initialize(appBuild: AppBuild?) {
        if(fetchJob?.isActive == true)
            return

        if(this.currentAppBuild == null)
            this.currentAppBuild = appBuild

        fetchJob = scope.launch {
            val retryDelay = 3000L
            for (i in 0..MAX_RETRIES) {
                _configurationStatus.update { Resource.Loading }

                try {
                    checkForUpdates()

                    homeCategoriesData = githubRawApiService.getHomeCategoriesConfig()
                    searchCategoriesData = githubRawApiService.getSearchCategoriesConfig()

                    return@launch _configurationStatus.update { Resource.Success(Unit) }
                } catch (e: Exception) {
                    errorLog(e)

                    if (i == MAX_RETRIES) {
                        val errorMessageId = e.catchInternetRelatedException().error!!

                        return@launch _configurationStatus.update { Resource.Failure(errorMessageId) }
                    }
                }

                delay(retryDelay)
            }

            _configurationStatus.update {
                Resource.Failure(UtilR.string.failed_to_init_app)
            }
        }
    }

    override suspend fun checkForUpdates() {
        _updateStatus.update { UpdateStatus.Fetching }

        try {
            val appSettings = appSettingsManager.appSettings.data.first()
            val isUsingPrereleaseUpdates = appSettings.isUsingPrereleaseUpdates

            appConfig = githubRawApiService.getAppConfig()

            if(appConfig!!.isMaintenance)
                return _updateStatus.update { UpdateStatus.Maintenance }

            if (isUsingPrereleaseUpdates && currentAppBuild?.debug == false) {
                val lastCommitObject = githubApiService.getLastCommitObject()
                val appCommitVersion = currentAppBuild?.commitVersion
                    ?: throw NullPointerException("appCommitVersion should not be null!")

                val preReleaseTag = "pre-release"
                val preReleaseTagInfo = githubApiService.getTagsInfo().find { it.name == preReleaseTag }

                val shortenedSha = lastCommitObject.lastCommit.sha.shortenSha()
                val isNeedingAnUpdate = appCommitVersion != shortenedSha
                    && lastCommitObject.lastCommit.sha == preReleaseTagInfo?.lastCommit?.sha

                if (isNeedingAnUpdate) {
                    val preReleaseReleaseInfo = githubApiService.getReleaseInfo(tag = preReleaseTag)

                    appConfig = appConfig!!.copy(
                        versionName = "PR-$shortenedSha \uD83D\uDDFF",
                        updateInfo = preReleaseReleaseInfo.releaseNotes,
                        updateUrl = "https://github.com/$GITHUB_USERNAME/$GITHUB_REPOSITORY/releases/download/pre-release/flixclusive-release.apk"
                    )

                    _updateStatus.update { UpdateStatus.Outdated }
                    return
                }

                _updateStatus.update { UpdateStatus.UpToDate }
                return
            } else {
                val isNeedingAnUpdate = appConfig!!.build != -1L && appConfig!!.build > currentAppBuild!!.build

                if(isNeedingAnUpdate) {
                    val releaseInfo = githubApiService.getReleaseInfo(tag = appConfig!!.versionName)

                    appConfig = appConfig!!.copy(updateInfo = releaseInfo.releaseNotes)
                    return _updateStatus.update { UpdateStatus.Outdated }
                }

                return _updateStatus.update { UpdateStatus.UpToDate }
            }
        } catch (e: Exception) {
            errorLog(e)
            val errorMessageId = e.catchInternetRelatedException().error!!

            _updateStatus.update { UpdateStatus.Error(errorMessageId) }
        }
    }

    private fun String.shortenSha()
        = substring(0, 7)
}