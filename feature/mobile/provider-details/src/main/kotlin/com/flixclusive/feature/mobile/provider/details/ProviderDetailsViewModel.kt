package com.flixclusive.feature.mobile.provider.details

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.common.locale.UiText
import com.flixclusive.core.common.provider.ProviderInstallationStatus
import com.flixclusive.core.common.provider.ProviderWithThrowable
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.datastore.model.user.ProviderPreferences
import com.flixclusive.core.datastore.model.user.UserPreferences
import com.flixclusive.core.navigation.navargs.ProviderMetadataNavArgs
import com.flixclusive.core.network.util.Resource
import com.flixclusive.core.util.log.infoLog
import com.flixclusive.data.provider.repository.ProviderRepository
import com.flixclusive.domain.provider.usecase.get.GetProviderFromRemoteUseCase
import com.flixclusive.domain.provider.usecase.manage.LoadProviderResult
import com.flixclusive.domain.provider.usecase.manage.LoadProviderUseCase
import com.flixclusive.domain.provider.usecase.manage.UnloadProviderUseCase
import com.flixclusive.domain.provider.usecase.updater.UpdateProviderUseCase
import com.flixclusive.model.provider.ProviderMetadata
import com.flixclusive.model.provider.Repository.Companion.toValidRepositoryLink
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class ProviderDetailsViewModel
    @Inject
    constructor(
        private val dataStoreManager: DataStoreManager,
        private val loadProvider: LoadProviderUseCase,
        private val unloadProvider: UnloadProviderUseCase,
        private val _updateProvider: UpdateProviderUseCase,
        private val providerRepository: ProviderRepository,
        private val getProviderFromRemote: GetProviderFromRemoteUseCase,
        private val appDispatchers: AppDispatchers,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val navArgs = savedStateHandle.navArgs<ProviderMetadataNavArgs>()

        private var providerJob: Job? = null

        private val _uiState = MutableStateFlow(ProviderDetailsUiState(navArgs.metadata))
        val uiState = _uiState.asStateFlow()

        val warnOnInstall = dataStoreManager
            .getUserPrefs(UserPreferences.PROVIDER_PREFS_KEY, ProviderPreferences::class)
            .map { it.shouldWarnBeforeInstall }
            .distinctUntilChanged()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = false,
            )

        init {
            viewModelScope.launch {
                initialize()
            }
        }

        private suspend fun initialize() {
            onInstallationStatusChange(ProviderInstallationStatus.NotInstalled)

            try {
                val isInstalledAlready = providerRepository.getProviderMetadata(navArgs.metadata.id) != null
                if (isInstalledAlready && isOutdated(navArgs.metadata)) {
                    onInstallationStatusChange(ProviderInstallationStatus.Outdated)
                } else if (isInstalledAlready) {
                    onInstallationStatusChange(ProviderInstallationStatus.Installed)
                }
            } catch (e: Throwable) {
                _uiState.update {
                    it.copy(initializationError = UiText.from(e.stackTraceToString()))
                }
            }
        }

        fun onToggleInstallation() {
            if (providerJob?.isActive == true) return

            providerJob = appDispatchers.ioScope.launch {
                onConsumeInstallationError()

                with(_uiState.value) {
                    val provider = metadata
                    when (installationStatus) {
                        ProviderInstallationStatus.NotInstalled -> installProvider(provider)
                        ProviderInstallationStatus.Outdated -> updateProvider(provider)
                        ProviderInstallationStatus.Installed -> uninstallProvider(provider)
                        else -> Unit
                    }
                }
            }
        }

        private suspend fun installProvider(provider: ProviderMetadata) {
            loadProvider(metadata = provider)
                .onStart {
                    infoLog("Downloading and installing provider: ${provider.name}")
                    onInstallationStatusChange(ProviderInstallationStatus.Installing)
                }.onEach { result ->
                    if (result is LoadProviderResult.Failure) {
                        throw result.error
                    }
                }.catch { e ->
                    val error = ProviderWithThrowable(provider = provider, throwable = e)
                    _uiState.update { it.copy(installationError = error) }
                }.onCompletion {
                    // There is a good case that the provider was installed successfully,
                    // but an error was thrown after the installation.
                    // So we check if the provider is installed or not.
                    val isInstalled = providerRepository.getProviderMetadata(provider.id) != null
                    val status = when (isInstalled) {
                        true -> ProviderInstallationStatus.Installed
                        false -> ProviderInstallationStatus.NotInstalled
                    }

                    onInstallationStatusChange(status)
                }.collect()
        }

        private suspend fun uninstallProvider(provider: ProviderMetadata) {
            try {
                // Simulate loading state for better UX
                infoLog("Uninstalling provider: ${provider.name}")
                onInstallationStatusChange(ProviderInstallationStatus.Installing)

                unloadProvider(provider)
                onInstallationStatusChange(ProviderInstallationStatus.NotInstalled)
            } catch (e: Throwable) {
                val error = ProviderWithThrowable(provider = provider, throwable = e)
                _uiState.update { it.copy(installationError = error) }
            }
        }

        private suspend fun updateProvider(provider: ProviderMetadata) {
            try {
                infoLog("Updating and installing provider: ${provider.name}")
                onInstallationStatusChange(ProviderInstallationStatus.Installing)

                _updateProvider(provider)
                onInstallationStatusChange(ProviderInstallationStatus.Installed)
            } catch (e: Throwable) {
                val error = ProviderWithThrowable(provider = provider, throwable = e)
                _uiState.update { it.copy(installationError = error) }
            }
        }

        fun onConsumeInstallationError() {
            _uiState.update { it.copy(installationError = null) }
        }

        private fun onInstallationStatusChange(status: ProviderInstallationStatus) {
            _uiState.update { it.copy(installationStatus = status) }
        }

        fun disableWarnOnInstall(state: Boolean) {
            appDispatchers.ioScope.launch {
                dataStoreManager.updateUserPrefs(
                    key = UserPreferences.PROVIDER_PREFS_KEY,
                    type = ProviderPreferences::class,
                ) {
                    it.copy(shouldWarnBeforeInstall = state)
                }
            }
        }

        private suspend fun isOutdated(old: ProviderMetadata): Boolean {
            val provider = providerRepository.getProvider(old.id) ?: return false

            val oldManifest = provider.manifest
            if (oldManifest.updateUrl == null || oldManifest.updateUrl.equals("")) {
                return false
            }

            val repository = old.repositoryUrl.toValidRepositoryLink()
            val resource = getProviderFromRemote(repository, old.id)
            if (resource !is Resource.Success || resource.data == null) {
                return false
            }

            val new = resource.data!!

            return old.versionCode < new.versionCode
        }
    }

@Immutable
internal data class ProviderDetailsUiState(
    val metadata: ProviderMetadata,
    val installationStatus: ProviderInstallationStatus = ProviderInstallationStatus.NotInstalled,
    val initializationError: UiText? = null,
    val installationError: ProviderWithThrowable? = null,
)
