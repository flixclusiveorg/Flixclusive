package com.flixclusive.data.configuration

import com.flixclusive.core.datastore.AppSettingsManager
import com.flixclusive.core.network.retrofit.FlixclusiveConfigurationService
import com.flixclusive.core.util.common.dispatcher.di.ApplicationScope
import com.flixclusive.core.util.common.resource.Resource
import com.flixclusive.core.util.exception.catchInternetRelatedException
import com.flixclusive.core.util.log.errorLog
import com.flixclusive.data.provider.ProviderRepository
import com.flixclusive.model.configuration.AppConfig
import com.flixclusive.model.configuration.HomeCategoriesConfig
import com.flixclusive.model.configuration.ProviderStatus
import com.flixclusive.model.configuration.SearchCategoriesConfig
import com.flixclusive.model.datastore.ProviderPreference
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
    private val appConfigService: FlixclusiveConfigurationService,
    private val providersRepository: ProviderRepository,
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
    override var providersStatus: List<ProviderStatus>? = null

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

                    providersStatus = appConfigService.getProvidersStatus()
                    initializeProviders()

                    homeCategoriesConfig = appConfigService.getHomeCategoriesConfig()
                    searchCategoriesConfig = appConfigService.getSearchCategoriesConfig()

                    if (homeCategoriesConfig == null || searchCategoriesConfig == null || appConfig == null || providersStatus == null) {
                        continue
                    }

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
        try {
            _updateStatus.emit(UpdateStatus.Fetching)

            appConfig = appConfigService.getAppConfig()
            appConfig?.run {
                if(isMaintenance)
                    return _updateStatus.emit(UpdateStatus.Maintenance)

                currentAppBuild?.let {
                    val isNeedingAnUpdate = build != -1L && build > it.build
                    if(isNeedingAnUpdate) {
                        return _updateStatus.emit(UpdateStatus.Outdated)
                    }
                }

                return _updateStatus.emit(UpdateStatus.UpToDate)
            }
        } catch (e: Exception) {
            errorLog(e.stackTraceToString())
            val errorMessageId = e.catchInternetRelatedException().error!!

            _updateStatus.emit(UpdateStatus.Error(errorMessageId))
        }
    }

    private suspend fun initializeProviders() {
        val appSettings = appSettingsManager.localAppSettings
        val providersPreferences = appSettings.providers.toMutableList()

        var isConfigEmpty = providersPreferences.isEmpty()
        val isNotInitializedCorrectly = providersPreferences.size < providersStatus!!.size

        if (providersRepository.providers.size >= providersStatus!!.size)
            return

        providersRepository.providers.clear()
        if (!isConfigEmpty && isNotInitializedCorrectly) {
            providersPreferences.clear()
            isConfigEmpty = true
        }

        for (i in providersStatus!!.indices) {
            val provider = if (isConfigEmpty) {
                providersStatus!![i]
            } else {
                providersStatus!!.find {
                    it.name.equals(
                        other = providersPreferences[i].name,
                        ignoreCase = true
                    )
                }
            }

            val isIgnored = providersPreferences.getOrNull(i)?.isIgnored ?: false

            providersRepository.populate(
                name = provider!!.name,
                isMaintenance = provider.isMaintenance,
                isIgnored = isIgnored
            )

            if(isConfigEmpty) {
                providersPreferences.add(
                    ProviderPreference(
                        provider.name
                    )
                )
            }
        }

        appSettingsManager.updateData(
            appSettings.copy(providers = providersPreferences)
        )
    }
}