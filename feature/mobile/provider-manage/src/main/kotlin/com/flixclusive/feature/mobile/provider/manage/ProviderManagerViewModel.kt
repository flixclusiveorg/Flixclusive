package com.flixclusive.feature.mobile.provider.manage

import androidx.compose.runtime.Immutable
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.common.provider.ProviderWithThrowable
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.core.datastore.model.user.ProviderPreferences
import com.flixclusive.core.datastore.model.user.UserOnBoarding
import com.flixclusive.core.datastore.model.user.UserPreferences
import com.flixclusive.core.util.log.warnLog
import com.flixclusive.data.provider.repository.ProviderRepository
import com.flixclusive.domain.provider.usecase.manage.UnloadProviderUseCase
import com.flixclusive.model.provider.ProviderMetadata
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
internal class ProviderManagerViewModel @Inject constructor(
    private val unloadProvider: UnloadProviderUseCase,
    private val dataStoreManager: DataStoreManager,
    private val userSessionDataStore: UserSessionDataStore,
    private val providerRepository: ProviderRepository,
    private val appDispatchers: AppDispatchers,
) : ViewModel() {
    private var uninstallJob: Job? = null
    private var toggleJob: Job? = null

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


    val providers = combine(
        userSessionDataStore.currentUserId.filterNotNull(),
        _uiState.map { it.isSearching }.distinctUntilChanged(),
        searchQuery,
    ) { userId, isSearching, query ->
        Triple(userId, isSearching, query)
    }.flatMapLatest { (userId, isSearching, query) ->
        providerRepository
            .getInstalledProvidersAsFlow(ownerId = userId)
            .map { list ->
                list.mapNotNull { provider ->
                    providerRepository.getMetadata(provider.id)
                }.run {
                    if (isSearching) {
                        return@run this@run
                    }

                    fastFilter { metadata ->
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
            started = SharingStarted.Eagerly,
            initialValue = false,
        )

    val providerToggles = dataStoreManager
        .getUserPrefs(UserPreferences.PROVIDER_PREFS_KEY, ProviderPreferences::class)
        .map { prefs -> prefs.providers.fastMap { it.isDisabled } }
        .distinctUntilChanged()
        .stateIn(
            viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList(),
        )

    fun onQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    suspend fun onMove(
        fromIndex: Int,
        toIndex: Int,
    ) {
        val userId = userSessionDataStore.currentUserId.filterNotNull().first()
        providerRepository.moveProvider(
            from = fromIndex,
            to = toIndex,
            ownerId = userId
        )
    }

    fun toggleProvider(id: String) {
        if (toggleJob?.isActive == true) return

        toggleJob =
            appDispatchers.ioScope.launch {
                val userId = userSessionDataStore.currentUserId.filterNotNull().first()

                providerRepository.toggleProvider(id = id, ownerId = userId)
                val isEnabled = providerRepository.isEnabled(id = id, ownerId = userId)

                if (!isEnabled) return@launch

                try {
                    providerRepository.getApi(
                        id = id,
                        ownerId = userId,
                    )
                } catch (e: Exception) {
                    warnLog("Failed to load provider with id $id after toggling it on, disabling it again.")
                    _uiState.update {
                        it.copy(
                            error = ProviderWithThrowable(
                                provider = providerRepository.getMetadata(id)!!,
                                throwable = e
                            )
                        )
                    }
                }
            }
    }

    fun uninstallProvider(metadata: ProviderMetadata) {
        if (uninstallJob?.isActive == true) return

        uninstallJob = appDispatchers.ioScope.launch {
            val userId = userSessionDataStore.currentUserId.filterNotNull().first()
            val provider = providerRepository.getInstalledProvider(
                id = metadata.id,
                ownerId = userId
            )

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
    val error: ProviderWithThrowable? = null,
)
