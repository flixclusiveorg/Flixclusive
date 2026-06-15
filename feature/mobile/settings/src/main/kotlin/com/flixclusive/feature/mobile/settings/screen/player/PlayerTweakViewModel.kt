package com.flixclusive.feature.mobile.settings.screen.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.datastore.DataStoreManager.Companion.getUserPrefs
import com.flixclusive.core.datastore.DataStoreManager.Companion.getUserPrefsAsFlow
import com.flixclusive.core.datastore.DataStoreManager.Companion.updateUserPrefs
import com.flixclusive.core.datastore.model.user.PlayerPreferences
import com.flixclusive.core.datastore.model.user.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltViewModel
internal class PlayerTweakViewModel @Inject constructor(
    private val dataStoreManager: DataStoreManager,
    private val appDispatchers: AppDispatchers,
) : ViewModel() {
    val preferences = dataStoreManager
        .getUserPrefsAsFlow<PlayerPreferences>(UserPreferences.PLAYER_PREFS_KEY)
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = runBlocking {
                dataStoreManager.getUserPrefs(UserPreferences.PLAYER_PREFS_KEY)
            },
        )

    fun updateUserPrefs(
        transform: suspend (t: PlayerPreferences) -> PlayerPreferences,
    ) {
        appDispatchers.ioScope.launch {
            dataStoreManager.updateUserPrefs(key = UserPreferences.PLAYER_PREFS_KEY, transform = transform)
        }
    }
}
