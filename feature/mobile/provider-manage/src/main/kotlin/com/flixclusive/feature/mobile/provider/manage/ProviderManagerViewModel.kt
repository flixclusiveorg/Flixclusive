package com.flixclusive.feature.mobile.provider.manage

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.compose.ui.util.fastFilter
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.common.locale.UiText
import com.flixclusive.core.common.provider.ProviderWithThrowable
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.core.datastore.model.user.UserOnBoarding
import com.flixclusive.core.datastore.model.user.UserPreferences
import com.flixclusive.core.util.log.warnLog
import com.flixclusive.data.provider.repository.ProviderRepository
import com.flixclusive.domain.provider.usecase.get.GetInstalledProviderUseCase
import com.flixclusive.domain.provider.usecase.manage.UnloadProviderUseCase
import com.flixclusive.model.provider.ProviderMetadata
import com.flixclusive.provider.ProviderPlugin
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
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

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
        .debounce(800)
        .distinctUntilChanged()
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = _searchQuery.value,
        )

    private val installedProviders = userSessionDataStore
        .currentUserId
        .filterNotNull()
        .flatMapLatest { userId ->
            providerRepository.getProvidersAsFlow(ownerId = userId)
        }
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    val providers = combine(
        _uiState.map { it.isSearching }.distinctUntilChanged(),
        searchQuery,
    ) { isSearching, query ->
        isSearching to query
    }.flatMapLatest { (isSearching, query) ->
        installedProviders
            .mapLatest { list ->
                list.mapNotNull { provider ->
                    val metadata = provider.metadata ?: return@mapNotNull null
                    val plugin = provider.plugin ?: return@mapNotNull null

                    ProviderWithCapabilities(
                        metadata = metadata,
                        capabilities = getCapabilities(plugin, metadata)
                    )
                }.let { metadataList ->
                    if (isSearching) {
                        return@let metadataList
                    }

                    metadataList.fastFilter { metadata ->
                        metadata.name.contains(query, ignoreCase = true)
                    }
                }
            }
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = emptyList(),
        )

    val isFirstTimeOnProvidersScreen = dataStoreManager
        .getUserPrefs(UserPreferences.USER_ON_BOARDING_PREFS_KEY, UserOnBoarding::class)
        .map { it.isFirstTimeOnProvidersScreen }
        .distinctUntilChanged()
        .stateIn(
            viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = false,
        )

    private suspend fun getCapabilities(
        plugin: ProviderPlugin,
        metadata: ProviderMetadata,
    ): List<UiText> {
        return withContext(appDispatchers.io) {
            try {
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

                        if (hasStreamsAndSubs) {
                            add(UiText.from(R.string.label_provider_capability_links))
                        } else if (linksApi.supportedLinkTypes.contains(MediaLinkType.STREAMS)) {
                            add(UiText.from(R.string.label_provider_capability_streams))
                        } else if (linksApi.supportedLinkTypes.contains(MediaLinkType.SUBTITLES)) {
                            add(UiText.from(R.string.label_provider_capability_subs))
                        }
                    }

                    if (catalogApi != null) add(UiText.from(R.string.label_provider_capability_catalogs))
                    if (trackerApi != null) add(UiText.from(R.string.label_provider_capability_tracking))
                    if (searchApi != null) add(UiText.from(R.string.label_provider_capability_search))
                    if (metadataApi != null) add(UiText.from(R.string.label_provider_capability_metadata))
                    if (crossMatchApi != null) add(UiText.from(R.string.label_provider_capability_cross_match))
                }
            } catch (e: Throwable) {
                _uiState.update {
                    val currentErrors = it.error ?: emptyList()
                    it.copy(error = currentErrors + ProviderWithThrowable(metadata, e))
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

            unloadProvider(provider)
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
        _uiState.update { it.copy(error = null) }
    }

    fun onToggleSearchBar(state: Boolean) {
        _uiState.update { it.copy(isSearching = state) }
    }
}

@Immutable
internal data class ProviderManageUiState(
    val isSearching: Boolean = false,
    val error: List<ProviderWithThrowable>? = null,
)

@Immutable
internal data class ProviderWithCapabilities(
    val metadata: ProviderMetadata,
    val capabilities: List<UiText> = emptyList(),
) {
    val id: String get() = metadata.id
    val name get() = metadata.name
}
