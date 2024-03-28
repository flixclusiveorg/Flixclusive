package com.flixclusive.feature.mobile.provider

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.datastore.AppSettingsManager
import com.flixclusive.data.provider.ProviderManager
import com.flixclusive.provider.Provider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProvidersScreenViewModel @Inject constructor(
    private val providerManager: ProviderManager,
    appSettingsManager: AppSettingsManager,
) : ViewModel() {
    val providerDataMap = providerManager.providerDataMap
    val providers: List<Provider>
        get() = providerManager.providers.values.toList()

    var isSearching by mutableStateOf(false)

    val appSettings = appSettingsManager.providerSettings
        .data
        .map { it.providers }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = appSettingsManager.localProviderSettings.providers
        )

    fun onMove(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            providerManager.swap(fromIndex, toIndex)
        }
    }

    fun toggleProvider(index: Int) {
        viewModelScope.launch {
            providerManager.toggleUsage(index)
        }
    }

    fun uninstallProvider(name: String) {
        viewModelScope.launch {
            providerManager.unloadProvider(name)
        }
    }
}
