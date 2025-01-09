package com.flixclusive.feature.mobile.provider

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.datastore.util.asStateFlow
import com.flixclusive.core.util.coroutines.AppDispatchers
import com.flixclusive.core.util.coroutines.asStateFlow
import com.flixclusive.core.util.log.errorLog
import com.flixclusive.data.provider.ProviderApiRepository
import com.flixclusive.data.provider.ProviderManager
import com.flixclusive.data.provider.ProviderRepository
import com.flixclusive.model.datastore.user.ProviderPreferences
import com.flixclusive.model.datastore.user.UserOnBoarding
import com.flixclusive.model.datastore.user.UserPreferences
import com.flixclusive.model.provider.ProviderMetadata
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

internal data class ProviderError(
    val providerId: String,
    val error: Throwable,
)

@HiltViewModel
internal class ProvidersScreenViewModel
    @Inject
    constructor(
        private val providerManager: ProviderManager,
        private val dataStoreManager: DataStoreManager,
        private val providerRepository: ProviderRepository,
        private val providerApiRepository: ProviderApiRepository,
    ) : ViewModel() {
        private val ioScope = AppDispatchers.IO.scope
        val providers = mutableStateListOf<ProviderMetadata>()

        val providersChangesHandler =
            ProvidersOperationsHandler(
                repository = providerRepository,
                providers = providers,
            )

        private var uninstallJob: Job? = null
        private var toggleJob: Job? = null

        private val _error = MutableSharedFlow<ProviderError?>()
        val error: SharedFlow<ProviderError?> = _error.asSharedFlow()

        var searchQuery by mutableStateOf("")
            private set

        val userOnBoardingPrefs =
            dataStoreManager
                .getUserPrefs<UserOnBoarding>(UserPreferences.USER_ON_BOARDING_PREFS_KEY)
                .asStateFlow(viewModelScope)

        val providerPrefs =
            dataStoreManager
                .getUserPrefs<ProviderPreferences>(UserPreferences.PROVIDER_PREFS_KEY)
                .map { it.providers.map { it.isDisabled } }
                .distinctUntilChanged()
                .asStateFlow(viewModelScope, initialValue = emptyList())

        init {
            viewModelScope.launch {
                providers.addAll(providerRepository.getOrderedProviders())

                providerRepository.observePositions().collect { operation ->
                    providersChangesHandler.handleOperations(operation)
                }
            }
        }

        fun onSearchQueryChange(newQuery: String) {
            searchQuery = newQuery
        }

        fun onMove(
            fromIndex: Int,
            toIndex: Int,
        ) {
            ioScope.launch {
                providerRepository.moveProvider(fromIndex, toIndex)
            }
        }

        fun toggleProvider(id: String) {
            if (toggleJob?.isActive == true) return

            toggleJob =
                ioScope.launch {
                    providerRepository.toggleProvider(id = id)
                    val isDisabled =
                        providerRepository
                            .getProviderFromPreferences(id = id)
                            ?.isDisabled == true

                    if (isDisabled) {
                        providerApiRepository.removeApi(id)
                    } else {
                        try {
                            val api = providerApiRepository.getApi(id) ?: return@launch
                            providerApiRepository.addApi(
                                id = id,
                                api = api,
                            )
                        } catch (e: Throwable) {
                            providerRepository.toggleProvider(id = id)
                            errorLog(e)
                            _error.emit(
                                ProviderError(
                                    providerId = id,
                                    error = e,
                                ),
                            )
                        }
                    }
                }
        }

        fun uninstallProvider(metadata: ProviderMetadata) {
            if (uninstallJob?.isActive == true) return

            uninstallJob =
                ioScope.launch {
                    providerManager.unload(metadata)
                }
        }

        suspend fun setFirstTimeOnProvidersScreen(state: Boolean) {
            dataStoreManager.updateUserPrefs<UserOnBoarding>(UserPreferences.USER_ON_BOARDING_PREFS_KEY) {
                it.copy(
                    isFirstTimeOnProvidersScreen = state,
                )
            }
        }
    }
