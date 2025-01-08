package com.flixclusive.feature.mobile.provider

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.datastore.util.asStateFlow
import com.flixclusive.core.util.coroutines.AppDispatchers
import com.flixclusive.data.provider.ProviderManager
import com.flixclusive.model.datastore.user.UserOnBoarding
import com.flixclusive.model.datastore.user.UserPreferences
import com.flixclusive.model.provider.ProviderMetadata
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class ProvidersScreenViewModel
    @Inject
    constructor(
        private val providerManager: ProviderManager,
        private val dataStoreManager: DataStoreManager,
    ) : ViewModel() {
        val providerPreferencesAsState = providerManager.providerPreferencesAsState
        val providers by derivedStateOf {
            providerPreferencesAsState.value.providers.mapNotNull {
                providerManager.metadataList[it.id]
            }
        }

        private var uninstallJob: Job? = null
        private var toggleJob: Job? = null
        private var swapJob: Job? = null

        var searchQuery by mutableStateOf("")
            private set

        val userOnBoardingPrefs =
            dataStoreManager
                .getUserPrefs<UserOnBoarding>(UserPreferences.USER_ON_BOARDING_PREFS_KEY)
                .asStateFlow(viewModelScope)

        fun onSearchQueryChange(newQuery: String) {
            searchQuery = newQuery
        }

        fun onMove(
            fromIndex: Int,
            toIndex: Int,
        ) {
            if (swapJob?.isActive == true) {
                return
            }

            swapJob =
                viewModelScope.launch {
                    providerManager.swapOrder(fromIndex, toIndex)
                }
        }

        fun toggleProvider(providerMetadata: ProviderMetadata) {
            if (toggleJob?.isActive == true) {
                return
            }

            toggleJob =
                viewModelScope.launch {
                    providerManager.toggleUsage(providerMetadata)
                }
        }

        fun uninstallProvider(metadata: ProviderMetadata) {
            if (uninstallJob?.isActive == true) {
                return
            }

            uninstallJob =
                AppDispatchers.IO.scope.launch {
                    with(providerManager) {
                        unload(metadata)
                    }
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
