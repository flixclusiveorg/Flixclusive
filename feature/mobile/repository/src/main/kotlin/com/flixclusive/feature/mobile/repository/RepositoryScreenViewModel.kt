package com.flixclusive.feature.mobile.repository

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.locale.UiText
import com.flixclusive.core.network.util.Resource
import com.flixclusive.core.ui.common.navigation.navargs.RepositoryScreenNavArgs
import com.flixclusive.core.ui.mobile.component.provider.ProviderInstallationStatus
import com.flixclusive.core.util.coroutines.AppDispatchers
import com.flixclusive.core.util.log.errorLog
import com.flixclusive.data.provider.ProviderManager
import com.flixclusive.data.provider.util.DownloadFailed
import com.flixclusive.domain.provider.GetOnlineProvidersUseCase
import com.flixclusive.domain.updater.ProviderUpdaterUseCase
import com.flixclusive.model.datastore.user.ProviderPreferences
import com.flixclusive.model.datastore.user.UserPreferences
import com.flixclusive.model.provider.ProviderMetadata
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.flixclusive.core.locale.R as LocaleR

@HiltViewModel
internal class RepositoryScreenViewModel
    @Inject
    constructor(
        private val providerManager: ProviderManager,
        private val providerUpdaterUseCase: ProviderUpdaterUseCase,
        private val getOnlineProvidersUseCase: GetOnlineProvidersUseCase,
        private val dataStoreManager: DataStoreManager,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        val repository = savedStateHandle.navArgs<RepositoryScreenNavArgs>().repository

        var uiState by mutableStateOf<Resource<List<ProviderMetadata>>>(Resource.Loading)
            private set
        var snackbar by mutableStateOf<Resource.Failure?>(null)
            private set

        var searchQuery by mutableStateOf("")
            private set
        val onlineProviderMap = mutableStateMapOf<ProviderMetadata, ProviderInstallationStatus>()

        val warnOnInstall =
            providerManager.providerPreferencesAsState
                .map { it.shouldWarnBeforeInstall }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000L),
                    initialValue = false,
                )

        private var initJob: Job? = null
        private var installJob: Job? = null
        var installAllJob: Job? by mutableStateOf(null)
            private set

        init {
            initialize()
        }

        fun initialize() {
            if (initJob?.isActive == true) {
                return
            }

            initJob =
                viewModelScope.launch {
                    try {
                        uiState = Resource.Loading

                        val onlineProviders = getOnlineProvidersUseCase(repository)
                        onlineProviderMap.clear()

                        uiState = onlineProviders
                        if (onlineProviders is Resource.Success) {
                            onlineProviders.data!!.forEach { provider ->
                                var providerInstallationStatus = ProviderInstallationStatus.NotInstalled

                                val isInstalledAlready =
                                    providerManager.metadataList[provider.id] != null

                                if (isInstalledAlready && providerUpdaterUseCase.isOutdated(provider.id)) {
                                    providerInstallationStatus = ProviderInstallationStatus.Outdated
                                } else if (isInstalledAlready) {
                                    providerInstallationStatus = ProviderInstallationStatus.Installed
                                }

                                onlineProviderMap[provider] = providerInstallationStatus
                            }
                        }
                    } catch (e: Exception) {
                        errorLog(e)
                        uiState = Resource.Failure(e.localizedMessage)
                    }
                }
        }

        fun installAll() {
            if (installAllJob?.isActive == true) {
                return
            }

            installAllJob =
                AppDispatchers.Default.scope.launch {
                    val failedToInstallProviders = arrayListOf<String>()

                    onlineProviderMap.forEach { (data, state) ->
                        if (state == ProviderInstallationStatus.Installed) {
                            return@forEach
                        }

                        if (!installProvider(data)) {
                            failedToInstallProviders.add(data.name)
                            return@forEach
                        }
                    }

                    if (failedToInstallProviders.isNotEmpty()) {
                        val failedProviders = failedToInstallProviders.joinToString(", ")

                        snackbar =
                            Resource.Failure(
                                UiText.StringResource(LocaleR.string.failed_to_load_provider, failedProviders),
                            )
                        return@launch
                    }

                    snackbar = Resource.Failure(UiText.StringResource(LocaleR.string.all_providers_installed))
                }
        }

        fun toggleInstallationStatus(providerMetadata: ProviderMetadata) {
            if (installJob?.isActive == true) {
                return
            }

            installJob =
                AppDispatchers.Default.scope.launch {
                    when (onlineProviderMap[providerMetadata]) {
                        ProviderInstallationStatus.NotInstalled -> installProvider(providerMetadata)
                        ProviderInstallationStatus.Installed -> uninstallProvider(providerMetadata)
                        ProviderInstallationStatus.Outdated -> updateProvider(providerMetadata)
                        else -> Unit
                    }
                }
        }

        private suspend fun updateProvider(providerMetadata: ProviderMetadata) {
            try {
                providerUpdaterUseCase.update(providerMetadata.name)
                onlineProviderMap[providerMetadata] = ProviderInstallationStatus.Installed
            } catch (_: DownloadFailed) {
                snackbar =
                    Resource.Failure(UiText.StringResource(LocaleR.string.failed_to_update_provider))
            }
        }

        private suspend fun installProvider(providerMetadata: ProviderMetadata): Boolean {
            onlineProviderMap[providerMetadata] = ProviderInstallationStatus.Installing

            try {
                providerManager.loadProvider(
                    provider = providerMetadata,
                    needsDownload = true,
                )
            } catch (_: Exception) {
                snackbar =
                    Resource.Failure(
                        UiText.StringResource(LocaleR.string.failed_to_load_provider, providerMetadata.name),
                    )
                onlineProviderMap[providerMetadata] = ProviderInstallationStatus.NotInstalled
                return false
            }

            val isInstalled = providerManager.providers[providerMetadata.id] != null
            if (isInstalled) {
                onlineProviderMap[providerMetadata] = ProviderInstallationStatus.Installed
                return true
            }

            return false
        }

        private suspend fun uninstallProvider(providerMetadata: ProviderMetadata) {
            providerManager.unload(providerMetadata)
            onlineProviderMap[providerMetadata] = ProviderInstallationStatus.NotInstalled
        }

        fun onSearchQueryChange(newQuery: String) {
            searchQuery = newQuery
        }

        fun onConsumeSnackbar() {
            snackbar = null
        }

        fun disableWarnOnInstall(state: Boolean) {
            viewModelScope.launch {
                dataStoreManager.updateUserPrefs<ProviderPreferences>(UserPreferences.PROVIDER_PREFS_KEY) {
                    it.copy(
                        shouldWarnBeforeInstall = state,
                    )
                }
            }
        }
    }
