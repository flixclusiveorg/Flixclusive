package com.flixclusive.feature.mobile.provider

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.datastore.AppSettingsManager
import com.flixclusive.data.provider.ProviderManager
import com.flixclusive.gradle.entities.ProviderData
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

    var searchQuery by mutableStateOf("")
        private set

    val providerSettings = appSettingsManager.providerSettings
        .data
        .map { it.providers }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = appSettingsManager.localProviderSettings.providers
        )

    fun onSearchQueryChange(newQuery: String) {
        searchQuery = newQuery
    }

    fun onMove(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            providerManager.swap(fromIndex, toIndex)
        }
    }

    fun toggleProvider(providerData: ProviderData) {
        viewModelScope.launch {
            providerManager.toggleUsage(providerData)
        }
    }

    fun uninstallProvider(providerData: ProviderData) {
        viewModelScope.launch {
            providerManager.unloadProvider(providerData)
        }
    }
}
