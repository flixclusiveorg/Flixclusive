package com.flixclusive.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.flixclusive.domain.preferences.VideoDataServerPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private const val USER_SERVER_PREFERENCES = "userServerPreferences"

class VideoDataServerPreferencesImpl @Inject constructor(
    private val context: Context
) : VideoDataServerPreferences {
    override val Context.dataStore: DataStore<Preferences> by preferencesDataStore(USER_SERVER_PREFERENCES)
    override val userPreferredServerKey: Preferences.Key<String>
        get() = stringPreferencesKey("user_preferred_server")

    override val getPreferredServer: Flow<String?>
        get() = context.dataStore.data.map { preferences ->
            preferences[userPreferredServerKey]
        }

    override suspend fun savePreferredServer(serverName: String) {
        context.dataStore.edit { preferences ->
            preferences[userPreferredServerKey] = serverName
        }
    }
}