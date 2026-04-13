package com.flixclusive.feature.mobile.onboarding

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
internal class OnboardingViewModel @Inject constructor(
    private val dataStoreManager: DataStoreManager,
    private val appDispatchers: AppDispatchers,
) : ViewModel() {
    val systemPreferences = dataStoreManager
        .getSystemPrefs()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SystemPreferences(),
        )

    fun updateStorageDirectoryUri(uri: String?) {
        appDispatchers.ioScope.launch {
            dataStoreManager.updateSystemPrefs {
                it.copy(storageDirectoryUri = uri)
            }
        }
    }

    fun completeOnboarding() {
        appDispatchers.ioScope.launch {
            dataStoreManager.updateSystemPrefs {
                it.copy(isFirstTimeUserLaunch = false)
            }
        }
    }
}
