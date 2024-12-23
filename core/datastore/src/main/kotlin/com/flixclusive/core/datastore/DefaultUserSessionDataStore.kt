package com.flixclusive.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

internal class DefaultUserSessionDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) : UserSessionDataStore {
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = USER_SESSION_FILE_NAME)

    override val currentUserId: Flow<Int?> = context.dataStore.data.map { preferences ->
        preferences[CURRENT_USER_ID_PREF_KEY]
    }

    override val sessionTimeout: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[SESSION_TIMEOUT_PREF_KEY] ?: 0L
    }

    private companion object {
        val CURRENT_USER_ID_PREF_KEY = intPreferencesKey(CURRENT_USER_ID_KEY)
        val SESSION_TIMEOUT_PREF_KEY = longPreferencesKey(SESSION_TIMEOUT_ID_KEY)

        const val USER_SESSION_FILE_NAME = "user_session"
        const val SESSION_TIMEOUT = 4 * 60 * 60 * 1000L // 4 HOURS
    }

    override suspend fun saveCurrentUserId(userId: Int) {
        context.dataStore.edit { preferences ->
            preferences[CURRENT_USER_ID_PREF_KEY] = userId

            val sessionTimeoutMillis = System.currentTimeMillis() + SESSION_TIMEOUT
            preferences[SESSION_TIMEOUT_PREF_KEY] = sessionTimeoutMillis
        }
    }

    override suspend fun clearCurrentUser() {
        context.dataStore.edit { preferences ->
            preferences.remove(CURRENT_USER_ID_PREF_KEY)
            preferences.remove(SESSION_TIMEOUT_PREF_KEY)
        }
    }
}