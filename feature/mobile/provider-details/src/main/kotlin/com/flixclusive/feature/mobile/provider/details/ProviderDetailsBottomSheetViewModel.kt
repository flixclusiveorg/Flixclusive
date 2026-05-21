package com.flixclusive.feature.mobile.provider.details

import androidx.compose.runtime.Stable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.common.locale.UiText
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.datastore.model.user.ProviderPreferences
import com.flixclusive.core.datastore.model.user.UserPreferences
import com.flixclusive.core.navigation.navargs.ProviderMetadataNavArgs
import com.flixclusive.domain.provider.usecase.get.GetInstalledProviderUseCase
import com.flixclusive.domain.provider.usecase.get.GetProviderFromRemoteUseCase
import com.flixclusive.domain.provider.usecase.get.GetProviderPluginUseCase
import com.flixclusive.domain.provider.usecase.manage.DownloadProviderResult
import com.flixclusive.domain.provider.usecase.manage.InstallProviderUseCase
import com.flixclusive.domain.provider.usecase.manage.LoadProviderUseCase
import com.flixclusive.domain.provider.usecase.manage.ProviderResult
import com.flixclusive.domain.provider.usecase.manage.UnloadProviderUseCase
import com.flixclusive.domain.provider.usecase.updater.UpdateProviderUseCase
import com.flixclusive.model.provider.ProviderMetadata
import com.flixclusive.model.provider.Repository.Companion.toValidRepositoryLink
import com.ramcosta.composedestinations.generated.providerdetails.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class ProviderDetailsBottomSheetViewModel @Inject constructor(
    private val dataStoreManager: DataStoreManager,
    private val loadProvider: LoadProviderUseCase,
    private val installProvider: InstallProviderUseCase,
    private val unloadProvider: UnloadProviderUseCase,
    private val updateProvider: UpdateProviderUseCase,
    private val getInstalledProvider: GetInstalledProviderUseCase,
    private val getPlugin: GetProviderPluginUseCase,
    private val getProviderFromRemote: GetProviderFromRemoteUseCase,
    private val appDispatchers: AppDispatchers,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val navArgs = savedStateHandle.navArgs<ProviderMetadataNavArgs>()

    private var providerJob: Job? = null

    private val _installState = MutableStateFlow<InstallState>(InstallState.Loading)
    val installState = _installState.asStateFlow()

    private val _errors = MutableSharedFlow<UiText>()
    val errors = _errors.asSharedFlow()

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
        try {
            val isInstalledAlready = getInstalledProvider(navArgs.metadata.id) != null
            val state = if (isInstalledAlready) {
                val newVersion = getNewVersion(navArgs.metadata)

                newVersion ?: InstallState.Installed
            } else {
                InstallState.NotInstalled
            }

            _installState.value = state
        } catch (e: Throwable) {
            // This catch only expects errors from the update check, so we can
            // still show the provider details even if the update check fails
            _errors.emit(
                UiText.from(
                    R.string.error_msg_failed_to_check_for_updates,
                    e.message ?: "Unknown error"
                )
            )

            _installState.value = InstallState.Installed
        }
    }

    private suspend fun onInstallProvider(provider: ProviderMetadata) {
        val initialState = _installState.value

        try {
            _installState.value = InstallState.Installing(progress = 0f)
            installProvider(provider).collect {
                if (it is DownloadProviderResult.Failure) throw it.error
                if (it is DownloadProviderResult.Downloading) {
                    _installState.value = InstallState.Installing(
                        progress = it.progress.coerceIn(0f, 99f)
                    )
                }
            }

            val installedProvider = getInstalledProvider(provider.id)!!

            loadProvider(installedProvider).collect {
                if (it is ProviderResult.Failure) throw it.error
            }

            _installState.value = InstallState.Installed
        } catch (e: Throwable) {
            _errors.emit(
                UiText.from(
                    R.string.error_msg_failed_to_install_provider,
                    provider.name,
                    e.message ?: "Unknown error"
                )
            )

            val isInstalled = getInstalledProvider(provider.id) != null
            _installState.value = if (isInstalled) InstallState.Installed else initialState
        }
    }

    private suspend fun onUninstallProvider(provider: ProviderMetadata) {
        try {
            _installState.value = InstallState.Uninstalling
            val installedProvider = getInstalledProvider(provider.id)
            if (installedProvider != null) {
                unloadProvider(installedProvider)
                _installState.value = InstallState.NotInstalled
                return
            }

            _installState.value = InstallState.NotInstalled
            _errors.emit(
                UiText.from(
                    R.string.error_msg_skip_uninstall,
                    provider.name
                )
            )
        } catch (e: Throwable) {
            _installState.value = InstallState.Installed
            _errors.emit(
                UiText.from(
                    R.string.error_msg_failed_to_uninstall_provider,
                    e.message ?: "Unknown error"
                )
            )
        }
    }

    private suspend fun onUpdateProvider(provider: ProviderMetadata) {
        try {
            updateProvider(provider).collect {
                if (it is DownloadProviderResult.Failure) throw it.error
                if (it is DownloadProviderResult.Downloading) {
                    _installState.value = InstallState.Installing(
                        progress = it.progress
                    )
                }
                if (it is DownloadProviderResult.Success) {
                    _installState.value = InstallState.Installed
                }
            }
        } catch (e: Throwable) {
            _installState.value = InstallState.Installed
            _errors.emit(
                UiText.from(
                    R.string.error_msg_failed_to_update_provider,
                    e.message ?: "Unknown error"
                )
            )
        }
    }

    private suspend fun getNewVersion(local: ProviderMetadata): InstallState.Outdated? {
        val provider = getPlugin(local.id) ?: return null

        val oldManifest = provider.manifest
        if (oldManifest.updateUrl == null || oldManifest.updateUrl.equals("")) {
            return null
        }

        val repository = local.repositoryUrl.toValidRepositoryLink()
        val remote = getProviderFromRemote(repository, local.id)

        if (local.versionCode >= remote.versionCode) {
            return null
        }

        return InstallState.Outdated(
            newVersion = remote.versionName,
            newChangelogs = remote.changelog
        )
    }

    fun onUninstall() {
        if (providerJob?.isActive == true) return

        providerJob = viewModelScope.launch {
            onUninstallProvider(navArgs.metadata)
        }
    }

    fun onToggleInstallState() {
        if (providerJob?.isActive == true) return

        providerJob = viewModelScope.launch {
            when (_installState.value) {
                is InstallState.NotInstalled -> onInstallProvider(navArgs.metadata)
                is InstallState.Installed -> onUninstallProvider(navArgs.metadata)
                is InstallState.Outdated -> onUpdateProvider(navArgs.metadata)
                else -> {
                    // No-op
                }
            }
        }
    }

    fun onDisableInstallationWarning(state: Boolean) {
        appDispatchers.ioScope.launch {
            dataStoreManager.updateUserPrefs(
                key = UserPreferences.PROVIDER_PREFS_KEY,
                type = ProviderPreferences::class,
            ) {
                it.copy(shouldWarnBeforeInstall = state)
            }
        }
    }
}

@Stable
internal sealed class InstallState {
    data object Loading : InstallState()
    data object NotInstalled : InstallState()
    data object Installed : InstallState()
    data object Uninstalling : InstallState()
    data class Installing(val progress: Float) : InstallState() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Installing) return false

            return progress.toInt() == other.progress.toInt()
        }

        override fun hashCode(): Int {
            return progress.hashCode()
        }
    }
    data class Outdated(
        val newVersion: String,
        val newChangelogs: String?,
    ) : InstallState()
}
