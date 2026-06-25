package com.flixclusive.feature.mobile.provider.manage

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.compose.ui.util.fastFilter
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.common.locale.UiText
import com.flixclusive.core.common.provider.ProviderWithThrowable
import com.flixclusive.core.database.entity.provider.InstalledProvider
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.core.datastore.model.user.UserOnBoarding
import com.flixclusive.core.datastore.model.user.UserPreferences
import com.flixclusive.core.util.log.warnLog
import com.flixclusive.data.provider.repository.ProviderRepository
import com.flixclusive.data.provider.repository.ProviderResponseWrapper
import com.flixclusive.domain.provider.usecase.get.GetInstalledProviderUseCase
import com.flixclusive.domain.provider.usecase.manage.UnloadProviderUseCase
import com.flixclusive.model.provider.Language
import com.flixclusive.model.provider.ProviderMetadata
import com.flixclusive.model.provider.ProviderStatus
import com.flixclusive.model.provider.ProviderType
import com.flixclusive.provider.capability.MediaLinkType
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

import com.flixclusive.core.strings.R as LocaleR

@OptIn(FlowPreview::class)
@HiltViewModel
internal class ProviderManagerViewModel @Inject constructor(
    userSessionDataStore: UserSessionDataStore,
    @param:ApplicationContext private val context: Context,
    private val unloadProvider: UnloadProviderUseCase,
    private val dataStoreManager: DataStoreManager,
    private val getInstalledProvider: GetInstalledProviderUseCase,
    private val providerRepository: ProviderRepository,
    private val appDispatchers: AppDispatchers,
) : ViewModel() {
    private var uninstallJob: Job? = null

    private val _uiState = MutableStateFlow(ProviderManageUiState())
    val uiState = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery
        .debounce(800.milliseconds)
        .distinctUntilChanged()
        .stateIn(
            viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = _searchQuery.value,
        )

    private val installedProviders = userSessionDataStore
        .currentUserId
        .filterNotNull()
        .flatMapLatest { userId ->
            providerRepository
                .getProvidersAsFlow(ownerId = userId)
                .debounce(600.milliseconds)
        }

    val providers = combine(
        _uiState.map { it.isSearching }.distinctUntilChanged(),
        _searchQuery.debounce(800.milliseconds).distinctUntilChanged(),
        installedProviders,
    ) { isSearching, query, providers ->
        providers
            .map { wrapper ->
                ProviderWithCapabilities(
                    metadata = wrapper.metadata ?: getFallbackProviderMetadata(wrapper.provider),
                    capabilities = getCapabilities(wrapper)
                )
            }.let { metadataList ->
                if (!isSearching) {
                    return@let metadataList
                }

                metadataList.fastFilter { metadata ->
                    metadata.name.contains(query, ignoreCase = true)
                }
            }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = emptyList(),
    )

    val isFirstTimeOnProvidersScreen = dataStoreManager
        .getUserPrefsAsFlow(UserPreferences.USER_ON_BOARDING_PREFS_KEY, UserOnBoarding::class)
        .map { it.isFirstTimeOnProvidersScreen }
        .distinctUntilChanged()
        .stateIn(
            viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = false,
        )

    private fun getFallbackProviderMetadata(provider: InstalledProvider): ProviderMetadata {
        return ProviderMetadata(
            id = provider.id,
            name = provider.id,
            repositoryUrl = provider.repositoryUrl,
            buildUrl = "",
            versionName = "-1",
            versionCode = -1,
            language = Language.Multiple,
            providerType = ProviderType(context.getString(LocaleR.string.label_invalid)),
            status = ProviderStatus.Down,
        )
    }

    private suspend fun getCapabilities(
        wrapper: ProviderResponseWrapper,
    ): List<CapabilityUiItem> {
        return withContext(appDispatchers.io) {
            val metadata = wrapper.metadata ?: getFallbackProviderMetadata(wrapper.provider)

            try {
                val plugin = wrapper.plugin
                if (plugin == null || wrapper.metadata == null) {
                    warnLog("Plugin is null for provider with id ${wrapper.metadata?.id}, skipping capabilities check.")
                    _uiState.update {
                        it.copy(
                            errors = it.errors +
                                ProviderWithThrowable(
                                    provider = metadata,
                                    throwable = IllegalStateException(
                                        context.getString(R.string.error_missing_provider_plugin)
                                    )
                                )
                        )
                    }
                    return@withContext emptyList()
                }

                val catalogApi = plugin.getCatalogApi(context)
                val searchApi = plugin.getSearchApi(context)
                val metadataApi = plugin.getMetadataApi(context)
                val trackerApi = plugin.getTrackerApi(context)
                val linksApi = plugin.getMediaLinkApi(context)
                val crossMatchApi = plugin.getCrossMatchApi(context)

                buildList {
                    if (linksApi != null) {
                        val hasStreamsAndSubs = linksApi.supportedLinkTypes.containsAll(
                            listOf(MediaLinkType.STREAMS, MediaLinkType.SUBTITLES)
                        )

                        val label = when {
                            hasStreamsAndSubs -> UiText.from(R.string.label_provider_capability_links)

                            linksApi.supportedLinkTypes.contains(
                                MediaLinkType.STREAMS
                            ) -> UiText.from(R.string.label_provider_capability_streams)

                            linksApi.supportedLinkTypes.contains(
                                MediaLinkType.SUBTITLES
                            ) -> UiText.from(R.string.label_provider_capability_subs)

                            else -> null
                        }

                        if (label != null) {
                            add(CapabilityUiItem(label = label, isEnabled = wrapper.isMediaLinkEnabled))
                        }
                    }

                    if (catalogApi != null) {
                        add(
                            CapabilityUiItem(
                                UiText.from(R.string.label_provider_capability_catalogs),
                                wrapper.isCatalogEnabled
                            )
                        )
                    }
                    if (trackerApi != null) {
                        add(
                            CapabilityUiItem(
                                UiText.from(R.string.label_provider_capability_tracking),
                                wrapper.isTrackerEnabled
                            )
                        )
                    }
                    if (searchApi != null) {
                        add(
                            CapabilityUiItem(
                                UiText.from(R.string.label_provider_capability_search),
                                wrapper.isSearchEnabled
                            )
                        )
                    }
                    if (metadataApi != null) {
                        add(
                            CapabilityUiItem(
                                UiText.from(R.string.label_provider_capability_metadata),
                                wrapper.isMetadataEnabled
                            )
                        )
                    }
                    if (crossMatchApi != null) {
                        add(
                            CapabilityUiItem(
                                UiText.from(R.string.label_provider_capability_cross_match),
                                wrapper.isCrossMatchEnabled
                            )
                        )
                    }
                }
            } catch (e: Throwable) {
                _uiState.update {
                    it.copy(errors = it.errors + ProviderWithThrowable(metadata, e))
                }
                emptyList()
            }
        }
    }

    fun onQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun uninstallProvider(metadata: ProviderMetadata) {
        if (uninstallJob?.isActive == true) return

        uninstallJob = appDispatchers.ioScope.launch {
            val provider = getInstalledProvider(metadata.id)

            if (provider == null) {
                warnLog("Failed to get provider config for provider with id ${metadata.id}, aborting uninstall.")
                return@launch
            }

            try {
                unloadProvider(provider)
            } catch (e: Throwable) {
                _uiState.update {
                    it.copy(errors = it.errors + ProviderWithThrowable(metadata, e))
                }
            }
        }
    }

    fun setFirstTimeOnProvidersScreen(state: Boolean) {
        appDispatchers.ioScope.launch {
            dataStoreManager.updateUserPrefs(
                key = UserPreferences.USER_ON_BOARDING_PREFS_KEY,
                type = UserOnBoarding::class,
            ) {
                it.copy(isFirstTimeOnProvidersScreen = state)
            }
        }
    }

    fun onConsumeError() {
        _uiState.update { it.copy(errors = emptyList()) }
    }

    fun onToggleSearchBar(state: Boolean) {
        _uiState.update { it.copy(isSearching = state) }
    }
}

@Immutable
internal data class ProviderManageUiState(
    val isSearching: Boolean = false,
    val errors: List<ProviderWithThrowable> = emptyList(),
)

@Immutable
internal data class ProviderWithCapabilities(
    val metadata: ProviderMetadata,
    val capabilities: List<CapabilityUiItem> = emptyList(),
) {
    val id: String get() = metadata.id
    val name get() = metadata.name
}

@Immutable
internal data class CapabilityUiItem(
    val label: UiText,
    val isEnabled: Boolean,
)
