package com.flixclusive.feature.mobile.provider.manage

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.common.provider.ProviderWithThrowable
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.datastore.model.user.ProviderPreferences
import com.flixclusive.core.datastore.model.user.UserOnBoarding
import com.flixclusive.core.datastore.model.user.UserPreferences
import com.flixclusive.data.provider.repository.ProviderApiRepository
import com.flixclusive.data.provider.repository.ProviderRepository
import com.flixclusive.domain.provider.usecase.manage.UnloadProviderUseCase
import com.flixclusive.model.provider.ProviderMetadata
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
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
    private val providerRepository: ProviderRepository,
    private val providerApiRepository: ProviderApiRepository,
    private val appDispatchers: AppDispatchers,
) : ViewModel() {
    val providers = mutableStateListOf<ProviderMetadata>()

    val providersChangesHandler = ProvidersOperationsHandler(
        repository = providerRepository,
        providers = providers,
    )

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
        .map { it.providers.fastMap { it.isDisabled } }
        .distinctUntilChanged()
        .stateIn(
            viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList(),
        )

    init {
        viewModelScope.launch {
            providers.addAll(providerRepository.getOrderedProviders())

            providerRepository.observe().collect { operation ->
                providersChangesHandler.handleOperations(operation)
            }
        }
    }

    fun onQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    suspend fun onMove(
        fromIndex: Int,
        toIndex: Int,
    ) {
        providerRepository.moveProvider(fromIndex, toIndex)
    }

    fun toggleProvider(id: String) {
        if (toggleJob?.isActive == true) return

        toggleJob =
            appDispatchers.ioScope.launch {
                providerRepository.toggleProvider(id = id)
                val isDisabled = providerRepository
                    .getProviderFromPreferences(id = id)
                    ?.isDisabled == true

                if (isDisabled) {
                    providerApiRepository.removeApi(id)
                } else {
                    try {
                        providerApiRepository.addApiFromId(id = id)
                    } catch (e: Throwable) {
                        providerRepository.toggleProvider(id = id)
                        val metadata = providerRepository.getProviderMetadata(id)!!
                        val error = ProviderWithThrowable(provider = metadata, throwable = e)
                        _uiState.update { it.copy(error = error) }
                    }
                }
            }
    }

    fun uninstallProvider(metadata: ProviderMetadata) {
        if (uninstallJob?.isActive == true) return

        uninstallJob = appDispatchers.ioScope.launch {
            unloadProvider(metadata)
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
