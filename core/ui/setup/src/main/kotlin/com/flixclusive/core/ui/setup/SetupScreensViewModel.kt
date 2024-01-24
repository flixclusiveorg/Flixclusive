package com.flixclusive.core.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.data.configuration.AppConfigurationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SetupScreensViewModel @Inject constructor(
    private val appConfigurationManager: AppConfigurationManager,
) : ViewModel() {
    val newVersion: String?
        get() = appConfigurationManager.appConfig?.versionName
    val updateInfo: String?
        get() = appConfigurationManager.appConfig?.updateInfo
    val updateUrl: String?
        get() = appConfigurationManager.appConfig?.updateUrl

    val updateStatus = appConfigurationManager.updateStatus
    val configurationStatus = appConfigurationManager.configurationStatus

    fun checkForUpdates() {
        viewModelScope.launch { appConfigurationManager.checkForUpdates() }
    }
}