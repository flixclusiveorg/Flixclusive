package com.flixclusive.feature.mobile.provider.info

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.locale.UiText
import com.flixclusive.core.network.util.Resource
import com.flixclusive.core.ui.common.navigation.navargs.ProviderInfoScreenNavArgs
import com.flixclusive.core.ui.mobile.component.provider.ProviderInstallationStatus
import com.flixclusive.core.util.coroutines.AppDispatchers
import com.flixclusive.data.provider.ProviderManager
import com.flixclusive.data.provider.util.DownloadFailed
import com.flixclusive.domain.provider.GetRepositoryUseCase
import com.flixclusive.domain.provider.util.extractGithubInfoFromLink
import com.flixclusive.domain.updater.ProviderUpdaterUseCase
import com.flixclusive.model.datastore.user.ProviderPreferences
import com.flixclusive.model.datastore.user.UserPreferences
import com.flixclusive.model.provider.Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.flixclusive.core.locale.R as LocaleR

@HiltViewModel
internal class ProviderInfoScreenViewModel
    @Inject
    constructor(
        private val dataStoreManager: DataStoreManager,
        private val getRepositoryUseCase: GetRepositoryUseCase,
        private val providerManager: ProviderManager,
        private val providerUpdaterUseCase: ProviderUpdaterUseCase,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val argsMetadata = savedStateHandle.navArgs<ProviderInfoScreenNavArgs>().providerMetadata
        var providerMetadata by mutableStateOf(argsMetadata)
            private set

        var providerInstallationStatus by mutableStateOf(ProviderInstallationStatus.NotInstalled)
            private set
        var snackbar by mutableStateOf<Resource.Failure?>(null)
            private set

        private var providerJob: Job? = null

        var repository: Repository? by mutableStateOf(null)
            private set

        val warnOnInstall =
            providerManager.providerPreferencesAsState
                .map { it.warnOnInstall }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000L),
                    initialValue = false,
                )

        init {
            viewModelScope.launch {
                initialize()
            }
        }

        private suspend fun initialize() {
            providerInstallationStatus = ProviderInstallationStatus.NotInstalled

            val isInstalledAlready =
                providerManager.metadataList[argsMetadata.id] != null

            if (isInstalledAlready && providerUpdaterUseCase.isOutdated(argsMetadata.id)) {
                providerInstallationStatus = ProviderInstallationStatus.Outdated
                viewModelScope.launch {
                    // Run this asynchronously
                    providerMetadata = providerUpdaterUseCase
                        .getLatestMetadata(argsMetadata.id) ?: argsMetadata
                }
            } else if (isInstalledAlready) {
                providerInstallationStatus = ProviderInstallationStatus.Installed
            }

            getRepository(providerMetadata.repositoryUrl)
        }

        fun toggleInstallation() {
            if (providerJob?.isActive == true) {
                return
            }

            providerJob =
                AppDispatchers.Default.scope.launch {
                    when (providerInstallationStatus) {
                        ProviderInstallationStatus.NotInstalled -> installProvider()
                        ProviderInstallationStatus.Outdated -> updateProvider()
                        ProviderInstallationStatus.Installed -> uninstallProvider()
                        else -> Unit
                    }
                }
        }

        private suspend fun installProvider() {
            providerInstallationStatus = ProviderInstallationStatus.Installing

            try {
                providerManager.loadProvider(
                    provider = argsMetadata,
                    needsDownload = true,
                )
            } catch (_: Exception) {
                snackbar =
                    Resource.Failure(
                        UiText.StringResource(
                            LocaleR.string.failed_to_load_provider,
                            argsMetadata.name,
                        ),
                    )
                providerInstallationStatus = ProviderInstallationStatus.NotInstalled
                return
            }

            providerInstallationStatus = ProviderInstallationStatus.Installed
        }

        private suspend fun uninstallProvider() {
            providerManager.unload(argsMetadata)
            providerInstallationStatus = ProviderInstallationStatus.NotInstalled
        }

        private suspend fun updateProvider() {
            try {
                providerUpdaterUseCase.update(argsMetadata.name)
                providerInstallationStatus = ProviderInstallationStatus.Installed
            } catch (_: DownloadFailed) {
                snackbar =
                    Resource.Failure(UiText.StringResource(LocaleR.string.failed_to_update_provider))
            }
        }

        private suspend fun getRepository(url: String) {
            val (username, repositoryName) = extractGithubInfoFromLink(url) ?: return

            repository =
                providerManager.providerPreferences.repositories.find {
                    it.owner.equals(username, true) && it.name == repositoryName
                }

            if (repository != null) {
                return
            }

            when (val onlineRepository = getRepositoryUseCase(url)) {
                is Resource.Failure -> snackbar = onlineRepository
                Resource.Loading -> Unit
                is Resource.Success -> {
                    repository = onlineRepository.data
                    dataStoreManager.updateUserPrefs<ProviderPreferences>(UserPreferences.PROVIDER_PREFS_KEY) {
                        it.copy(repositories = it.repositories + repository!!)
                    }
                }
            }
        }

        fun onConsumeSnackbar() {
            snackbar = null
        }

        fun disableWarnOnInstall(state: Boolean) {
            viewModelScope.launch {
                dataStoreManager.updateUserPrefs<ProviderPreferences>(UserPreferences.PROVIDER_PREFS_KEY) {
                    it.copy(warnOnInstall = state)
                }
            }
        }
    }
