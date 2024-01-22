package com.flixclusive.core.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.datastore.AppSettingsManager
import com.flixclusive.data.configuration.AppConfigurationManager
import com.flixclusive.model.datastore.AppSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SetupScreensViewModel @Inject constructor(
    private val appConfigurationManager: AppConfigurationManager,
    private val appSettingsManager: AppSettingsManager,
) : ViewModel() {
    val newVersion: String?
        get() = appConfigurationManager.appConfig?.versionName
    val updateInfo: String?
        get() = appConfigurationManager.appConfig?.updateInfo
    val updateUrl: String?
        get() = appConfigurationManager.appConfig?.updateUrl

    val appSettings = appSettingsManager
        .appSettings.data
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = appSettingsManager.localAppSettings
        )

    val updateStatus = appConfigurationManager.updateStatus
    val configurationStatus = appConfigurationManager.configurationStatus

    fun checkForUpdates() {
        viewModelScope.launch { appConfigurationManager.checkForUpdates() }
    }

    fun updateSettings(newSettings: AppSettings) {
        viewModelScope.launch {
            appSettingsManager.updateData(newSettings)
        }
    }
}