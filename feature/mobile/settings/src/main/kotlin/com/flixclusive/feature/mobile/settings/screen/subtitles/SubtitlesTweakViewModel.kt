package com.flixclusive.feature.mobile.settings.screen.subtitles

import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.datastore.DataStoreManager.Companion.getUserPrefs
import com.flixclusive.core.datastore.DataStoreManager.Companion.getUserPrefsAsFlow
import com.flixclusive.core.datastore.DataStoreManager.Companion.updateUserPrefs
import com.flixclusive.core.datastore.model.user.SubtitlesPreferences
import com.flixclusive.core.datastore.model.user.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltViewModel
internal class SubtitlesTweakViewModel @Inject constructor(
    private val dataStoreManager: DataStoreManager,
    private val appDispatchers: AppDispatchers,
) : ViewModel() {
    val preferences = dataStoreManager
        .getUserPrefsAsFlow<SubtitlesPreferences>(UserPreferences.SUBTITLES_PREFS_KEY)
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = runBlocking {
                dataStoreManager.getUserPrefs(UserPreferences.SUBTITLES_PREFS_KEY)
            },
        )

    fun updateUserPrefs(
        transform: suspend (t: SubtitlesPreferences) -> SubtitlesPreferences,
    ) {
        appDispatchers.ioScope.launch {
            dataStoreManager.updateUserPrefs(key = UserPreferences.SUBTITLES_PREFS_KEY, transform = transform)
        }
    }

    inline fun <reified T : UserPreferences> updateUserPrefs(
        key: Preferences.Key<String>,
        noinline transform: suspend (t: T) -> T,
    ) {
        appDispatchers.ioScope.launch {
            dataStoreManager.updateUserPrefs(key = key, transform = transform)
        }
    }
}
