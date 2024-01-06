package com.flixclusive.data.configuration

import com.flixclusive.core.datastore.AppSettingsManager
import com.flixclusive.core.network.retrofit.FlixclusiveConfigurationService
import com.flixclusive.core.util.common.ui.UiText
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val MAX_RETRIES = 5

internal class DefaultAppConfigurationManager @Inject constructor(
    private val appConfigService: FlixclusiveConfigurationService,
    private val providersRepository: ProviderRepository,
    private val appSettingsManager: AppSettingsManager,
    private val ioScope: CoroutineScope,
) : AppConfigurationManager {

    private val _remoteStatus = MutableStateFlow<RemoteConfigStatus>(RemoteConfigStatus.Loading)
    private var fetchJob: Job? = null

    override val remoteStatus: StateFlow<RemoteConfigStatus>
        get() = _remoteStatus.asStateFlow()

    override var homeCategoriesConfig: HomeCategoriesConfig? = null
    override var searchCategoriesConfig: SearchCategoriesConfig? = null
    override var appConfig: AppConfig? = null
    override var providersStatus: List<ProviderStatus>? = null

    override fun initialize() {
        if(fetchJob?.isActive == true)
            return

        fetchJob = ioScope.launch {
            for (i in 0..MAX_RETRIES) {
                _remoteStatus.update { RemoteConfigStatus.Loading }

                try {
                    appConfig = appConfigService.getAppConfig()
                    providersStatus = appConfigService.getProvidersStatus()

                    initializeProviders()

                    homeCategoriesConfig = appConfigService.getHomeCategoriesConfig()
                    searchCategoriesConfig = appConfigService.getSearchCategoriesConfig()

                    if (homeCategoriesConfig == null || searchCategoriesConfig == null || appConfig == null || providersStatus == null) {
                        continue
                    }

                    return@launch _remoteStatus.update { RemoteConfigStatus.Success }
                } catch (e: Exception) {
                    errorLog(e.stackTraceToString())
                    val errorMessageId = e.catchInternetRelatedException().error!!

                    _remoteStatus.update { RemoteConfigStatus.Error(errorMessageId) }
                }

            }

            _remoteStatus.update {
                RemoteConfigStatus.Error(UiText.StringResource(R.string.failed_to_init_app))
            }
        }
    }

    override fun checkForUpdates() {
        if(fetchJob?.isActive == true)
            return

        fetchJob = ioScope.launch {
            _remoteStatus.update { RemoteConfigStatus.Loading }

            try {
                appConfig = appConfigService.getAppConfig()

                return@launch _remoteStatus.update { RemoteConfigStatus.Success }
            } catch (e: Exception) {
                errorLog(e.stackTraceToString())
                val errorMessageId = e.catchInternetRelatedException().error!!

                _remoteStatus.update { RemoteConfigStatus.Error(errorMessageId) }
            }
        }
    }

    private suspend fun initializeProviders() {
        val appSettings = appSettingsManager.localAppSettings
        val providersPreferences = appSettings.providers.toMutableList()

        val isConfigEmpty = providersPreferences.isEmpty()

        if (providersRepository.providers.size >= providersStatus!!.size)
            return

        providersRepository.providers.clear()

        for (i in providersStatus!!.indices) {
            val provider = if (!isConfigEmpty) {
                providersStatus!!.find {
                    it.name.equals(
                        other = providersPreferences[i].name,
                        ignoreCase = true
                    )
                }
            } else providersStatus!![i]

            val isIgnored = providersPreferences.getOrNull(i)?.isIgnored ?: false

            providersRepository.populate(
                name = provider!!.name,
                isMaintenance = provider.isMaintenance,
                isIgnored = isIgnored
            )

            if (isConfigEmpty) {
                providersPreferences.add(
                    ProviderPreference(name = provider.name)
                )

                appSettingsManager.updateData(
                    appSettings.copy(providers = providersPreferences)
                )
            }
        }
    }
}