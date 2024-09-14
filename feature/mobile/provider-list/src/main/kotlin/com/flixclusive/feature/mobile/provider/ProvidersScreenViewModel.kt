package com.flixclusive.feature.mobile.provider

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.datastore.AppSettingsManager
import com.flixclusive.data.provider.ProviderManager
import com.flixclusive.model.provider.ProviderData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class ProvidersScreenViewModel @Inject constructor(
    private val providerManager: ProviderManager,
    private val appSettingsManager: AppSettingsManager,
) : ViewModel() {
    val providerDataList = providerManager.providerDataList

    private var uninstallJob: Job? = null
    private var toggleJob: Job? = null
    private var swapJob: Job? = null

    var searchQuery by mutableStateOf("")
        private set

    val providerSettings = appSettingsManager.providerSettings
        .data
        .map { it.providers }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = appSettingsManager.cachedProviderSettings.providers
        )

    val isFirstTimeOnProvidersScreen = appSettingsManager.onBoardingPreferences
        .data
        .map { it.isFirstTimeOnProvidersScreen }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

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
            providerManager.unloadProvider(providerSettings.value[index])
        }
    }

    suspend fun setFirstTimeOnProvidersScreen(state: Boolean) {
        appSettingsManager.updateOnBoardingPreferences {
            it.copy(
                isFirstTimeOnProvidersScreen = state
            )
        }
    }
}
