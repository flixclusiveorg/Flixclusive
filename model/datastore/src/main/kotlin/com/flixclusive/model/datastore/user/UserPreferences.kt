package com.flixclusive.model.datastore.user

import androidx.datastore.preferences.core.stringPreferencesKey
import com.flixclusive.model.datastore.FlixclusivePrefs

interface UserPreferences : FlixclusivePrefs {
    companion object {
        val DATA_PREFS_KEY = stringPreferencesKey("data")
        val PLAYER_PREFS_KEY = stringPreferencesKey("player")
        val PROVIDER_PREFS_KEY = stringPreferencesKey("provider")
        val SUBTITLES_PREFS_KEY = stringPreferencesKey("subtitles")
        val UI_PREFS_KEY = stringPreferencesKey("ui")
        val USER_ON_BOARDING_PREFS_KEY = stringPreferencesKey("on_boarding")
    }
}