package com.flixclusive.feature.mobile.settings.screen.appearance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.datastore.DataStoreManager.Companion.getUserPrefs
import com.flixclusive.core.datastore.DataStoreManager.Companion.getUserPrefsAsFlow
import com.flixclusive.core.datastore.DataStoreManager.Companion.updateUserPrefs
import com.flixclusive.core.datastore.model.user.UiPreferences
import com.flixclusive.core.datastore.model.user.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltViewModel
internal class AppearanceTweakViewModel @Inject constructor(
    private val appDispatchers: AppDispatchers,
    private val dataStoreManager: DataStoreManager,
) : ViewModel() {
    val preferences = dataStoreManager
        .getUserPrefsAsFlow<UiPreferences>(UserPreferences.UI_PREFS_KEY)
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = runBlocking {
                dataStoreManager.getUserPrefs(UserPreferences.UI_PREFS_KEY)
            },
        )

    fun updateUserPrefs(
        transform: suspend (t: UiPreferences) -> UiPreferences,
    ) {
        appDispatchers.ioScope.launch {
            dataStoreManager.updateUserPrefs(key = UserPreferences.UI_PREFS_KEY, transform = transform)
        }
    }
}
