package com.flixclusive.data.config

import com.flixclusive.R
import com.flixclusive.common.UiText
import com.flixclusive.data.api.GithubConfigService
import com.flixclusive.data.utils.catchInternetRelatedException
import com.flixclusive.domain.config.ConfigurationProvider
import com.flixclusive.domain.config.RemoteConfigStatus
import com.flixclusive.domain.model.config.AppConfig
import com.flixclusive.domain.model.config.HomeCategoriesConfig
import com.flixclusive.domain.model.config.ProviderStatus
import com.flixclusive.domain.model.config.SearchCategoriesConfig
import com.flixclusive.domain.preferences.AppSettingsManager
import com.flixclusive.domain.preferences.ProviderConfiguration
import com.flixclusive.domain.repository.ProvidersRepository
import com.flixclusive.utils.LoggerUtils.errorLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val MAX_RETRIES = 5

class ConfigurationProviderImpl @Inject constructor(
    private val githubConfigService: GithubConfigService,
    private val providersRepository: ProvidersRepository,
    private val appSettingsManager: AppSettingsManager,
    private val ioScope: CoroutineScope,
) : ConfigurationProvider {

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
                    appConfig = githubConfigService.getAppConfig()
                    providersStatus = githubConfigService.getProvidersStatus()

                    initializeProviders()

                    homeCategoriesConfig = githubConfigService.getHomeCategoriesConfig()
                    searchCategoriesConfig = githubConfigService.getSearchCategoriesConfig()

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
                appConfig = githubConfigService.getAppConfig()

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
                    ProviderConfiguration(name = provider.name)
                )

                appSettingsManager.updateData(
                    appSettings.copy(providers = providersPreferences)
                )
            }
        }
    }
}