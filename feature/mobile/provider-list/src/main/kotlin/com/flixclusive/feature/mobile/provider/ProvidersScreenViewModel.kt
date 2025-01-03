package com.flixclusive.feature.mobile.provider

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.datastore.util.asStateFlow
import com.flixclusive.data.provider.ProviderManager
import com.flixclusive.model.datastore.user.UserOnBoarding
import com.flixclusive.model.datastore.user.UserPreferences
import com.flixclusive.model.provider.ProviderData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class ProvidersScreenViewModel @Inject constructor(
    private val providerManager: ProviderManager,
    private val dataStoreManager: DataStoreManager,
) : ViewModel() {
    val providerDataList = providerManager.providerDataList
    val providerPreferencesAsState = providerManager.providerPreferencesAsState

    private var uninstallJob: Job? = null
    private var toggleJob: Job? = null
    private var swapJob: Job? = null

    var searchQuery by mutableStateOf("")
        private set

    val userOnBoardingPrefs = dataStoreManager
        .getUserPrefs<UserOnBoarding>(UserPreferences.USER_ON_BOARDING_PREFS_KEY)
        .asStateFlow(viewModelScope)

    fun onSearchQueryChange(newQuery: String) {
        searchQuery = newQuery
    }

    fun onMove(fromIndex: Int, toIndex: Int) {
        if (swapJob?.isActive == true) {
            return
        }

        swapJob = viewModelScope.launch {
            providerManager.swapProvidersOrder(fromIndex, toIndex)
        }
    }

    fun toggleProvider(providerData: ProviderData) {
        if (toggleJob?.isActive == true) {
            return
        }

        toggleJob = viewModelScope.launch {
            providerManager.toggleUsage(providerData)
        }
    }

    fun uninstallProvider(index: Int) {
        if (uninstallJob?.isActive == true) {
            return
        }

        uninstallJob = viewModelScope.launch {
            with (providerManager) {
                unloadProvider(providerPreferencesAsState.first().providers[index])
            }
        }
    }

    suspend fun setFirstTimeOnProvidersScreen(state: Boolean) {
        dataStoreManager.updateUserPrefs<UserOnBoarding>(UserPreferences.USER_ON_BOARDING_PREFS_KEY) {
            it.copy(
                isFirstTimeOnProvidersScreen = state
            )
        }
    }
}
