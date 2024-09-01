package com.flixclusive.feature.mobile.provider.info

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.datastore.AppSettingsManager
import com.flixclusive.core.ui.common.navigation.ProviderInfoScreenNavArgs
import com.flixclusive.core.ui.mobile.component.provider.ProviderInstallationStatus
import com.flixclusive.core.util.common.dispatcher.di.ApplicationScope
import com.flixclusive.core.util.common.resource.Resource
import com.flixclusive.core.util.common.ui.UiText
import com.flixclusive.data.provider.ProviderManager
import com.flixclusive.domain.provider.GetRepositoryUseCase
import com.flixclusive.domain.provider.util.extractGithubInfoFromLink
import com.flixclusive.domain.updater.ProviderUpdaterUseCase
import com.flixclusive.gradle.entities.Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.flixclusive.core.util.R as UtilR

@HiltViewModel
class ProviderInfoScreenViewModel @Inject constructor(
    @ApplicationScope private val scope: CoroutineScope,
    private val appSettingsManager: AppSettingsManager,
    private val getRepositoryUseCase: GetRepositoryUseCase,
    private val providerManager: ProviderManager,
    private val providerUpdaterUseCase: ProviderUpdaterUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val oldProviderData = savedStateHandle.navArgs<ProviderInfoScreenNavArgs>().providerData
    var providerData by mutableStateOf(oldProviderData)
        private set

    var providerInstallationStatus by mutableStateOf(ProviderInstallationStatus.NotInstalled)
        private set
    var snackbar by mutableStateOf<Resource.Failure?>(null)
        private set

    private var providerJob: Job? = null

    var repository: Repository? by mutableStateOf(null)
        private set

    init {
        viewModelScope.launch {
            initialize()
        }
    }

    private suspend fun initialize() {
        providerInstallationStatus = ProviderInstallationStatus.NotInstalled

        val isInstalledAlready =
            providerManager.providerDataList.any { it.name.equals(oldProviderData.name, true) }

        if (isInstalledAlready && providerUpdaterUseCase.isProviderOutdated(oldProviderData)) {
            providerInstallationStatus = ProviderInstallationStatus.Outdated
            viewModelScope.launch {
                // Run this asynchronously
                providerData = providerUpdaterUseCase.getLatestProviderData(oldProviderData.name) ?: oldProviderData
            }
        } else if (isInstalledAlready) {
            providerInstallationStatus = ProviderInstallationStatus.Installed
        }

        getRepository(providerData.repositoryUrl ?: return)
    }

    fun toggleInstallation() {
        if (providerJob?.isActive == true)
            return

        providerJob = scope.launch {
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
                providerData = oldProviderData,
                needsDownload = true
            )
        } catch (_: Exception) {
            snackbar = Resource.Failure(
                UiText.StringResource(
                    UtilR.string.failed_to_load_provider,
                    oldProviderData.name
                )
            )
            providerInstallationStatus = ProviderInstallationStatus.NotInstalled
            return
        }

        providerInstallationStatus = ProviderInstallationStatus.Installed
    }

    private suspend fun uninstallProvider() {
        providerManager.unloadProvider(oldProviderData)
        providerInstallationStatus = ProviderInstallationStatus.NotInstalled
    }

    private suspend fun updateProvider() {
        val isSuccess = providerUpdaterUseCase.updateProvider(oldProviderData.name)

        if (isSuccess) {
            providerInstallationStatus = ProviderInstallationStatus.Installed
        } else {
            snackbar =
                Resource.Failure(UiText.StringResource(UtilR.string.failed_to_update_provider))
        }
    }

    private suspend fun getRepository(url: String) {
        val (username, repositoryName) = extractGithubInfoFromLink(url) ?: return

        repository = appSettingsManager.localProviderSettings.repositories.find {
            it.owner.equals(username, true) && it.name == repositoryName
        }

        if (repository != null)
            return

        when (val onlineRepository = getRepositoryUseCase(url)) {
            is Resource.Failure -> snackbar = onlineRepository
            Resource.Loading -> Unit
            is Resource.Success -> {
                repository = onlineRepository.data
                appSettingsManager.updateProviderSettings {
                    it.copy(repositories = it.repositories + repository!!)
                }
            }
        }
    }

    fun onConsumeSnackbar() {
        snackbar = null
    }
}
