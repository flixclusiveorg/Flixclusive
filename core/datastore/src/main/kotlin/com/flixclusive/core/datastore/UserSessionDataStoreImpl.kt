package com.flixclusive.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.flixclusive.core.datastore.UserSessionDataStoreImpl.Companion.USER_SESSION_FILE_NAME
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = USER_SESSION_FILE_NAME)

internal class UserSessionDataStoreImpl @Inject constructor(
    @param:ApplicationContext private val context: Context
) : UserSessionDataStore {
    override val currentUserId: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[CURRENT_USER_UUID_PREF_KEY]
    }

    override val sessionTimeout: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[SESSION_TIMEOUT_PREF_KEY] ?: 0L
    }

    @Deprecated("This field is only used for migration purposes and will be removed in future versions")
    override val legacyCurrentUserId: Flow<Int?> = context.dataStore.data.map { preferences ->
        preferences[CURRENT_USER_ID_PREF_KEY]
    }

    internal companion object {
        private const val SESSION_TIMEOUT_ID_KEY = "session_timeout"
        private const val CURRENT_USER_UUID_KEY = "user_session_uuid"
        val CURRENT_USER_UUID_PREF_KEY = stringPreferencesKey(CURRENT_USER_UUID_KEY)
        val SESSION_TIMEOUT_PREF_KEY = longPreferencesKey(SESSION_TIMEOUT_ID_KEY)

        const val USER_SESSION_FILE_NAME = "user_session"
        const val SESSION_TIMEOUT = 4 * 60 * 60 * 1000L // 4 HOURS


        // TODO: Remove this key in future versions after migration is complete
        @Deprecated("This key is only used for migration purposes and will be removed in future versions")
        private const val CURRENT_USER_ID_KEY = "user_session_id"

        // TODO: Remove legacy user ID in future versions after migration is complete
        @Deprecated("This field is only used for migration purposes and will be removed in future versions")
        val CURRENT_USER_ID_PREF_KEY = intPreferencesKey(CURRENT_USER_ID_KEY)
    }

    override suspend fun saveCurrentUserId(userId: String, legacyUserId: Int) {
        context.dataStore.edit { preferences ->
            preferences[CURRENT_USER_UUID_PREF_KEY] = userId
            preferences[CURRENT_USER_ID_PREF_KEY] = legacyUserId

            val sessionTimeoutMillis = System.currentTimeMillis() + SESSION_TIMEOUT
            preferences[SESSION_TIMEOUT_PREF_KEY] = sessionTimeoutMillis
        }
    }

    override suspend fun clearCurrentUser() {
        context.dataStore.edit { preferences ->
            preferences.remove(CURRENT_USER_UUID_PREF_KEY)
            preferences.remove(SESSION_TIMEOUT_PREF_KEY)
        }
    }
}
