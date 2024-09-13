package com.flixclusive.feature.mobile.about

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.datastore.AppSettingsManager
import com.flixclusive.data.configuration.AppConfigurationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
internal class AboutScreenViewModel @Inject constructor(
    appConfigurationManager: AppConfigurationManager,
    appSettingsManager: AppSettingsManager
) : ViewModel() {
    val currentAppBuild = appConfigurationManager.currentAppBuild
    val isOnPreRelease = appSettingsManager.appSettings
        .data.map { it.isUsingPrereleaseUpdates }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )
}