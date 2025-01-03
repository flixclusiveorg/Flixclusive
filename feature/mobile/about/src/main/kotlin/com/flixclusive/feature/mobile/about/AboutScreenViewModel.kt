package com.flixclusive.feature.mobile.about

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.datastore.util.awaitFirst
import com.flixclusive.data.configuration.AppConfigurationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
internal class AboutScreenViewModel @Inject constructor(
    appConfigurationManager: AppConfigurationManager,
    dataStoreManager: DataStoreManager
) : ViewModel() {
    val currentAppBuild = appConfigurationManager.currentAppBuild
    val isOnPreRelease = dataStoreManager.systemPreferences
        .data.map { it.isUsingPrereleaseUpdates }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = dataStoreManager.systemPreferences.awaitFirst().isUsingPrereleaseUpdates
        )
}