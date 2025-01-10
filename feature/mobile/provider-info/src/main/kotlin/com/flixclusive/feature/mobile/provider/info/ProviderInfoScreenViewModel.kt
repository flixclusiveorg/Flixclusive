package com.flixclusive.feature.mobile.provider.info

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.datastore.util.asStateFlow
import com.flixclusive.core.locale.UiText
import com.flixclusive.core.network.util.Resource
import com.flixclusive.core.ui.common.navigation.navargs.ProviderInfoScreenNavArgs
import com.flixclusive.core.ui.mobile.component.provider.ProviderInstallationStatus
import com.flixclusive.core.util.coroutines.AppDispatchers
import com.flixclusive.data.provider.ProviderRepository
import com.flixclusive.domain.provider.GetRepositoryUseCase
import com.flixclusive.domain.provider.ProviderLoaderUseCase
import com.flixclusive.domain.provider.ProviderUnloaderUseCase
import com.flixclusive.domain.provider.ProviderUpdaterUseCase
import com.flixclusive.domain.provider.util.DownloadFailed
import com.flixclusive.domain.provider.util.extractGithubInfoFromLink
import com.flixclusive.model.datastore.user.ProviderPreferences
import com.flixclusive.model.datastore.user.UserPreferences
import com.flixclusive.model.provider.Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.flixclusive.core.locale.R as LocaleR

@HiltViewModel
internal class ProviderInfoScreenViewModel
    @Inject
    constructor(
        private val dataStoreManager: DataStoreManager,
        private val getRepositoryUseCase: GetRepositoryUseCase,
        private val providerLoaderUseCase: ProviderLoaderUseCase,
        private val providerUnloaderUseCase: ProviderUnloaderUseCase,
        private val providerUpdaterUseCase: ProviderUpdaterUseCase,
        private val providerRepository: ProviderRepository,
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

        val providerPreferences =
            dataStoreManager
                .getUserPrefs<ProviderPreferences>(UserPreferences.PROVIDER_PREFS_KEY)
                .asStateFlow(viewModelScope)

        init {
            viewModelScope.launch {
                initialize()
            }
        }

        private suspend fun initialize() {
            providerInstallationStatus = ProviderInstallationStatus.NotInstalled

            val isInstalledAlready =
                providerRepository.getProviderMetadata(argsMetadata.id) != null

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
                providerLoaderUseCase.load(
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
            providerUnloaderUseCase.unload(argsMetadata)
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
                providerPreferences.value.repositories.find {
                    it.owner.equals(username, true) && it.name == repositoryName
                }

            if (repository != null) {
                return
            }

            when (val onlineRepository = getRepositoryUseCase(url)) {
                is Resource.Failure -> {
                    snackbar = onlineRepository
                }

                Resource.Loading -> {}

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
                    it.copy(shouldWarnBeforeInstall = state)
                }
            }
        }
    }
