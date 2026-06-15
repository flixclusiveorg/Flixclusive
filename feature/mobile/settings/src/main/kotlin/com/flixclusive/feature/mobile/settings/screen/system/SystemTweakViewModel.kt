package com.flixclusive.feature.mobile.settings.screen.system

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.datastore.model.system.SystemPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class SystemTweakViewModel @Inject constructor(
    private val dataStoreManager: DataStoreManager,
    private val appDispatchers: AppDispatchers,
) : ViewModel() {
    val preferences = dataStoreManager
        .getSystemPrefs()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SystemPreferences(),
        )

    fun updateSystemPrefs(transform: suspend (t: SystemPreferences) -> SystemPreferences) {
        appDispatchers.ioScope.launch {
            dataStoreManager.updateSystemPrefs(transform)
        }
    }
}
