package com.flixclusive.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val USER_SESSION_FILE_NAME = "user_session"

internal class DefaultUserSessionDataStore(
    private val context: Context
) : UserSessionDataStore {
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = USER_SESSION_FILE_NAME)

    override val currentUserId: Flow<Int?> = context.dataStore.data.map { preferences ->
        preferences[CURRENT_USER_ID_PREF_KEY]
    }

    companion object {
        private val CURRENT_USER_ID_PREF_KEY = intPreferencesKey(CURRENT_USER_ID_KEY)
    }

    override suspend fun saveCurrentUserId(userId: Int) {
        context.dataStore.edit { preferences ->
            preferences[CURRENT_USER_ID_PREF_KEY] = userId
        }
    }

    override suspend fun clearCurrentUser() {
        context.dataStore.edit { preferences ->
            preferences.remove(CURRENT_USER_ID_PREF_KEY)
        }
    }
}