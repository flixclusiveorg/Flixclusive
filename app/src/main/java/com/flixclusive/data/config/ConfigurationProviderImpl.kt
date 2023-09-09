package com.flixclusive.data.config

import com.flixclusive.common.LoggerUtils.errorLog
import com.flixclusive.data.api.GithubConfigService
import com.flixclusive.domain.config.ConfigurationProvider
import com.flixclusive.domain.config.RemoteConfigStatus
import com.flixclusive.domain.model.config.AppConfig
import com.flixclusive.domain.model.config.HomeCategoriesConfig
import com.flixclusive.domain.model.config.SearchCategoriesConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

class ConfigurationProviderImpl @Inject constructor(
    private val githubConfigService: GithubConfigService,
    private val ioScope: CoroutineScope
): ConfigurationProvider {
    private val _remoteStatus = MutableStateFlow(RemoteConfigStatus.LOADING)
    override val remoteStatus: StateFlow<RemoteConfigStatus>
        get() = _remoteStatus.asStateFlow()

    override var homeCategoriesConfig: HomeCategoriesConfig? = null
    override var searchCategoriesConfig: SearchCategoriesConfig? = null
    override var appConfig: AppConfig? = null

    override fun initialize() {
        ioScope.launch {
            _remoteStatus.update { RemoteConfigStatus.LOADING }

            try {
                appConfig = githubConfigService.getAppConfig()
                homeCategoriesConfig = githubConfigService.getHomeCategoriesConfig()
                searchCategoriesConfig = githubConfigService.getSearchCategoriesConfig()
                _remoteStatus.update { RemoteConfigStatus.SUCCESS }
            } catch (e: Exception) {
                _remoteStatus.update { RemoteConfigStatus.ERROR }
                errorLog(e.stackTraceToString())
            }
        }
    }
}