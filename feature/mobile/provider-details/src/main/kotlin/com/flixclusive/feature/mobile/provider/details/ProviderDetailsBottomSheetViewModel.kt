package com.flixclusive.feature.mobile.provider.details

import android.content.Context
import androidx.compose.runtime.Stable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.common.exception.ExceptionWithUiText
import com.flixclusive.core.common.locale.UiText
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.core.datastore.model.user.ProviderPreferences
import com.flixclusive.core.datastore.model.user.UserPreferences
import com.flixclusive.core.navigation.navargs.ProviderMetadataNavArgs
import com.flixclusive.core.presentation.mobile.components.provider.ProviderInstallState
import com.flixclusive.core.util.exception.safeCall
import com.flixclusive.core.util.log.errorLog
import com.flixclusive.data.provider.ProviderCapability
import com.flixclusive.data.provider.repository.ProviderRepository
import com.flixclusive.domain.downloads.usecase.CancelDownloadUseCase
import com.flixclusive.domain.provider.usecase.get.GetInstalledProviderUseCase
import com.flixclusive.domain.provider.usecase.get.GetProviderFromRemoteUseCase
import com.flixclusive.domain.provider.usecase.get.GetProviderPluginUseCase
import com.flixclusive.domain.provider.usecase.manage.DownloadProviderResult
import com.flixclusive.domain.provider.usecase.manage.InstallProviderUseCase
import com.flixclusive.domain.provider.usecase.manage.LoadProviderUseCase
import com.flixclusive.domain.provider.usecase.manage.ProviderResult
import com.flixclusive.domain.provider.usecase.manage.ToggleCapabilityUseCase
import com.flixclusive.domain.provider.usecase.manage.UnloadProviderUseCase
import com.flixclusive.domain.provider.usecase.updater.UpdateProviderUseCase
import com.flixclusive.model.provider.ProviderMetadata
import com.flixclusive.model.provider.Repository.Companion.toValidRepositoryLink
import com.flixclusive.provider.capability.MediaLinkType
import com.ramcosta.composedestinations.generated.providerdetails.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class ProviderDetailsBottomSheetViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val dataStoreManager: DataStoreManager,
    private val loadProvider: LoadProviderUseCase,
    private val installProvider: InstallProviderUseCase,
    private val unloadProvider: UnloadProviderUseCase,
    private val updateProvider: UpdateProviderUseCase,
    private val getInstalledProvider: GetInstalledProviderUseCase,
    private val getPlugin: GetProviderPluginUseCase,
    private val getProviderFromRemote: GetProviderFromRemoteUseCase,
    private val providerRepository: ProviderRepository,
    private val toggleCapabilityUseCase: ToggleCapabilityUseCase,
    private val appDispatchers: AppDispatchers,
    private val cancelDownload: CancelDownloadUseCase,
    userSessionDataStore: UserSessionDataStore,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val _installState = MutableStateFlow<ProviderInstallState>(ProviderInstallState.Loading)
    val installState = _installState.asStateFlow()

    private val _errors = MutableSharedFlow<UiText>()
    val errors = _errors.asSharedFlow()

    val warnOnInstall = dataStoreManager
        .getUserPrefsAsFlow(UserPreferences.PROVIDER_PREFS_KEY, ProviderPreferences::class)
        .map { it.shouldWarnBeforeInstall }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = false,
        )

    val capabilities = userSessionDataStore.currentUserId
        .filterNotNull()
        .flatMapLatest { userId ->
            providerRepository.getProviderAsFlow(navArgs.metadata.id, userId)
        }.mapLatest { wrapper ->
            if (wrapper == null || wrapper.plugin == null) return@mapLatest emptyList()
            val plugin = wrapper.plugin!!
            buildList {
                plugin.getMediaLinkApi(context)?.let { linksApi ->
                    val hasStreamsAndSubs = linksApi.supportedLinkTypes.containsAll(
                        listOf(MediaLinkType.STREAMS, MediaLinkType.SUBTITLES)
                    )
                    val containsStreams = linksApi.supportedLinkTypes.contains(MediaLinkType.STREAMS)
                    val containsSubs = linksApi.supportedLinkTypes.contains(MediaLinkType.SUBTITLES)
                    val label = when {
                        hasStreamsAndSubs -> UiText.from(R.string.label_provider_capability_links)
                        containsStreams -> UiText.from(R.string.label_provider_capability_streams)
                        containsSubs -> UiText.from(R.string.label_provider_capability_subs)
                        else -> null
                    }
                    val description = when {
                        hasStreamsAndSubs -> UiText.from(R.string.desc_provider_capability_links)
                        containsStreams -> UiText.from(R.string.desc_provider_capability_streams)
                        containsSubs -> UiText.from(R.string.desc_provider_capability_subs)
                        else -> null
                    }
                    if (label != null && description != null) {
                        add(
                            CapabilityItem(
                                capability = ProviderCapability.MEDIA_LINK,
                                label = label,
                                description = description,
                                isEnabled = wrapper.isMediaLinkEnabled,
                            )
                        )
                    }
                }
                if (plugin.getCatalogApi(context) != null) {
                    add(
                        CapabilityItem(
                            capability = ProviderCapability.CATALOG,
                            label = UiText.from(R.string.label_provider_capability_catalogs),
                            description = UiText.from(R.string.desc_provider_capability_catalogs),
                            isEnabled = wrapper.isCatalogEnabled,
                        )
                    )
                }
                if (plugin.getTrackerApi(context) != null) {
                    add(
                        CapabilityItem(
                            capability = ProviderCapability.TRACKER,
                            label = UiText.from(R.string.label_provider_capability_tracking),
                            description = UiText.from(R.string.desc_provider_capability_tracking),
                            isEnabled = wrapper.isTrackerEnabled,
                        )
                    )
                }
                if (plugin.getSearchApi(context) != null) {
                    add(
                        CapabilityItem(
                            capability = ProviderCapability.SEARCH,
                            label = UiText.from(R.string.label_provider_capability_search),
                            description = UiText.from(R.string.desc_provider_capability_search),
                            isEnabled = wrapper.isSearchEnabled,
                        )
                    )
                }
                if (plugin.getMetadataApi(context) != null) {
                    add(
                        CapabilityItem(
                            capability = ProviderCapability.METADATA,
                            label = UiText.from(R.string.label_provider_capability_metadata),
                            description = UiText.from(R.string.desc_provider_capability_metadata),
                            isEnabled = wrapper.isMetadataEnabled,
                        )
                    )
                }
                if (plugin.getCrossMatchApi(context) != null) {
                    add(
                        CapabilityItem(
                            capability = ProviderCapability.CROSS_MATCH,
                            label = UiText.from(R.string.label_provider_capability_cross_match),
                            description = UiText.from(R.string.desc_provider_capability_cross_match),
                            isEnabled = wrapper.isCrossMatchEnabled,
                        )
                    )
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    private val navArgs = savedStateHandle.navArgs<ProviderMetadataNavArgs>()

    private var providerJob: Job? = null

    init {
        viewModelScope.launch {
            initialize()
        }
    }

    private suspend fun initialize() {
        try {
            val isInstalledAlready = getInstalledProvider(navArgs.metadata.id) != null
            val state = if (isInstalledAlready) {
                val newVersion = getInstallState(navArgs.metadata)

                newVersion
            } else {
                ProviderInstallState.NotInstalled
            }

            _installState.value = state
        } catch (e: Throwable) {
            val message = when (e) {
                is ExceptionWithUiText -> e.uiText
                else -> null
            }

            _errors.emit(
                message
                    ?: UiText.from(R.string.error_msg_failed_to_check_for_updates, e.message ?: "Unknown error")
            )

            _installState.value = ProviderInstallState.Installed
        }
    }

    private suspend fun onInstallProvider(provider: ProviderMetadata) {
        val initialState = _installState.value

        try {
            installProvider(provider).collect {
                if (it is DownloadProviderResult.Failure) throw it.error
                if (it is DownloadProviderResult.Downloading) {
                    _installState.value = ProviderInstallState.Installing(
                        progress = it.progress.coerceIn(0f, 99f),
                        downloadId = it.downloadId,
                    )
                }
            }

            val installedProvider = getInstalledProvider(provider.id)!!

            loadProvider(installedProvider).collect {
                if (it is ProviderResult.Failure) throw it.error
            }

            _installState.value = ProviderInstallState.Installed
        } catch (e: Throwable) {
            _errors.emit(
                UiText.from(
                    R.string.error_msg_failed_to_install_provider,
                    provider.name,
                    e.message ?: "Unknown error"
                )
            )

            val isInstalled = getInstalledProvider(provider.id) != null
            _installState.value = if (isInstalled) ProviderInstallState.Installed else initialState
        }
    }

    private suspend fun onUninstallProvider(provider: ProviderMetadata) {
        try {
            _installState.value = ProviderInstallState.Uninstalling
            val installedProvider = getInstalledProvider(provider.id)
            if (installedProvider != null) {
                unloadProvider(installedProvider)
                _installState.value = ProviderInstallState.NotInstalled
                return
            }

            _installState.value = ProviderInstallState.NotInstalled
            _errors.emit(
                UiText.from(
                    R.string.error_msg_skip_uninstall,
                    provider.name
                )
            )
        } catch (e: Throwable) {
            _installState.value = ProviderInstallState.Installed
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
                    _installState.value = ProviderInstallState.Installing(
                        progress = it.progress,
                        downloadId = it.downloadId,
                    )
                }
                if (it is DownloadProviderResult.Success) {
                    _installState.value = ProviderInstallState.Installed
                }
            }
        } catch (e: Throwable) {
            errorLog(e.cause)

            _installState.value = safeCall {
                getInstallState(provider)
            } ?: ProviderInstallState.Installed

            _errors.emit(
                UiText.from(
                    R.string.error_msg_failed_to_update_provider,
                    e.message ?: "Unknown error"
                )
            )
        }
    }

    private suspend fun onCancelInstallation(downloadId: String) {
        try {
            cancelDownload(downloadId)

            val installState = getInstallState(navArgs.metadata)
            _installState.value = installState
        } catch (e: Throwable) {
            _errors.emit(UiText.from(R.string.error_failed_to_cancel_installation, e.message ?: "Unknown error"))
        }
    }

    private suspend fun getInstallState(local: ProviderMetadata): ProviderInstallState {
        val provider = getPlugin(local.id) ?: return ProviderInstallState.NotInstalled

        val oldManifest = provider.manifest
        if (oldManifest.updateUrl.isNullOrEmpty()) {
            return ProviderInstallState.NotInstalled
        }

        val repository = local.repositoryUrl.toValidRepositoryLink()
        val remote = getProviderFromRemote(repository, local.id)

        if (local.versionCode >= remote.versionCode) {
            return ProviderInstallState.Installed
        }

        return ProviderInstallState.Outdated(
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
        if (providerJob?.isActive == true && _installState.value !is ProviderInstallState.Installing) return

        providerJob = viewModelScope.launch {
            when (val state = _installState.value) {
                is ProviderInstallState.Installing -> onCancelInstallation(state.downloadId)
                is ProviderInstallState.Installed -> onUninstallProvider(navArgs.metadata)
                is ProviderInstallState.Outdated -> onUpdateProvider(navArgs.metadata)
                is ProviderInstallState.NotInstalled -> onInstallProvider(navArgs.metadata)
                else -> Unit
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

    fun toggleCapability(capability: ProviderCapability) {
        toggleCapabilityUseCase(id = navArgs.metadata.id, capability = capability)
    }
}

@Stable
internal data class CapabilityItem(
    val capability: ProviderCapability,
    val label: UiText,
    val description: UiText,
    val isEnabled: Boolean,
)
