package com.flixclusive.feature.mobile.plugin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.datastore.AppSettingsManager
import com.flixclusive.data.provider.PluginManager
import com.flixclusive.provider.base.plugin.Plugin
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PluginScreenViewModel @Inject constructor(
    private val pluginManager: PluginManager,
    appSettingsManager: AppSettingsManager,
) : ViewModel() {
    val pluginDataMap = pluginManager.pluginDataMap
    val plugins: List<Plugin>
        get() = pluginManager.plugins.values.toList()

    val appSettings = appSettingsManager.appSettings
        .data
        .map { it.plugins }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = appSettingsManager.localAppSettings.plugins
        )

    fun onMove(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            pluginManager.swap(fromIndex, toIndex)
        }
    }

    fun togglePlugin(index: Int) {
        viewModelScope.launch {
            pluginManager.toggleUsage(index)
        }
    }

    fun uninstallPlugin(name: String) {
        viewModelScope.launch {
            pluginManager.unloadPlugin(name)
        }
    }
}
