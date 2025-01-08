package com.flixclusive.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.flixclusive.core.datastore.migration.SystemPreferencesMigration
import com.flixclusive.core.datastore.migration.UserPreferencesMigration
import com.flixclusive.core.datastore.util.SystemPreferencesSerializer
import com.flixclusive.core.datastore.util.createUserPreferences
import com.flixclusive.core.util.coroutines.AppDispatchers.Companion.launchOnIO
import com.flixclusive.core.util.coroutines.AppDispatchers.Companion.withIOContext
import com.flixclusive.model.datastore.system.SystemPreferences
import com.flixclusive.model.datastore.user.UserPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

object NullUserPreferences : NullPointerException("User preferences cannot be null") {
    private fun readResolve(): Any = NullUserPreferences
}

internal const val SYSTEM_PREFS_FILENAME = "system-preferences.json"
private val Context.systemPreferences: DataStore<SystemPreferences> by dataStore(
    fileName = SYSTEM_PREFS_FILENAME,
    serializer = SystemPreferencesSerializer,
    produceMigrations = { context ->
        listOf(
            SystemPreferencesMigration(context),
        )
    },
)

class DataStoreManager
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val userSessionDataStore: UserSessionDataStore,
    ) {
        val systemPreferences = context.systemPreferences
        lateinit var userPreferences: DataStore<Preferences>

        init {
            launchOnIO {
                userSessionDataStore.currentUserId.collectLatest {
                    if (it != null) {
                        initUserPrefs(userId = it)
                    }
                }
            }
        }

        private fun initUserPrefs(userId: Int) {
            userPreferences =
                context.createUserPreferences(
                    userId = userId,
                    produceMigrations = { _ ->
                        listOf(
                            UserPreferencesMigration(context = context),
                        )
                    },
                )
        }

        inline fun <reified T : UserPreferences> getUserPrefs(key: Preferences.Key<String>): Flow<T> =
            userPreferences.data.map { preferences ->
                val data =
                    preferences[key]
                        ?: return@map T::class.java.getDeclaredConstructor().newInstance()

                Json.decodeFromString(data)
            }

        suspend inline fun <reified T : UserPreferences> updateUserPrefs(
            key: Preferences.Key<String>,
            crossinline transform: suspend (t: T) -> T,
        ) {
            userPreferences.edit { preferences ->
                val oldValue = preferences[key] ?: throw NullUserPreferences
                val newValue =
                    withIOContext {
                        transform(Json.decodeFromString(oldValue))
                    }

                preferences[key] = Json.encodeToString(newValue)
            }
        }

        suspend fun updateSystemPrefs(transform: suspend (t: SystemPreferences) -> SystemPreferences) {
            systemPreferences.updateData {
                val newSettings = transform(it)
                newSettings
            }
        }
    }
