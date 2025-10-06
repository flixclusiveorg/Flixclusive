package com.flixclusive.core.datastore

import android.content.Context
import androidx.annotation.GuardedBy
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStoreFile
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.datastore.migration.SystemPreferencesMigration
import com.flixclusive.core.datastore.migration.UserPreferencesMigration
import com.flixclusive.core.datastore.model.system.SystemPreferences
import com.flixclusive.core.datastore.model.user.UserPreferences
import com.flixclusive.core.datastore.serializer.system.SystemPreferencesSerializer
import com.flixclusive.core.datastore.util.USER_PREFERENCE_FILENAME
import com.flixclusive.core.datastore.util.createUserPreferences
import com.flixclusive.core.datastore.util.getProvidersPathPrefix
import com.flixclusive.core.datastore.util.getProvidersSettingsPathPrefix
import com.flixclusive.core.datastore.util.rmrf
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import java.io.File
import javax.inject.Inject
import kotlin.reflect.KClass

const val PROVIDERS_FOLDER_NAME = "providers"
const val PROVIDERS_SETTINGS_FOLDER_NAME = "settings"
internal const val SYSTEM_PREFS_FILENAME = "system-preferences.json"

internal val Context.systemPreferences: DataStore<SystemPreferences> by dataStore(
    fileName = SYSTEM_PREFS_FILENAME,
    serializer = SystemPreferencesSerializer,
    produceMigrations = { context ->
        listOf(
            SystemPreferencesMigration(context),
        )
    },
)

@OptIn(InternalSerializationApi::class)
internal class DataStoreManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userSessionDataStore: UserSessionDataStore,
    private val systemPreferences: DataStore<SystemPreferences>,
    private val appDispatchers: AppDispatchers
) : DataStoreManager {
    val lock = Any()

    @GuardedBy("lock")
    @Volatile
    private lateinit var userPreferences: DataStore<Preferences>

    init {
        CoroutineScope(appDispatchers.io).launch {
            userSessionDataStore.currentUserId.collectLatest {
                if (it != null) {
                    usePreferencesByUserId(userId = it)
                }
            }
        }
    }

    override fun getSystemPrefs() = systemPreferences.data

    override fun usePreferencesByUserId(userId: Int) {
        synchronized(lock) {
            userPreferences = context.createUserPreferences(
                userId = userId,
                produceMigrations = { _ ->
                    listOf(
                        UserPreferencesMigration(context = context),
                    )
                },
            )
        }
    }

    override fun <T : UserPreferences> getUserPrefs(
        key: Preferences.Key<String>,
        type: KClass<T>
    ): Flow<T> {
        return synchronized(lock) {
            userPreferences.data.map { preferences ->
                val data = preferences[key]
                val instance =
                    if (data != null) {
                        Json.decodeFromString(type.serializer(), data)
                    } else {
                        type.java.getDeclaredConstructor().newInstance()
                    }

                instance
            }
        }
    }

    override suspend fun <T : UserPreferences> updateUserPrefs(
        key: Preferences.Key<String>,
        type: KClass<T>,
        transform: suspend (T) -> T
    ) {
        withContext(appDispatchers.io) {
            userPreferences.edit { preferences ->
                val oldValue = preferences[key]
                val newValue =
                    if (oldValue != null) {
                        transform(Json.decodeFromString(type.serializer(), oldValue))
                    } else {
                        transform(type.java.getDeclaredConstructor().newInstance())
                    }

                preferences[key] = Json.encodeToString(type.serializer(), newValue)
            }
        }
    }

    override suspend fun updateSystemPrefs(transform: suspend (t: SystemPreferences) -> SystemPreferences) {
        withContext(appDispatchers.io) {
            systemPreferences.updateData {
                val newSettings = transform(it)
                newSettings
            }
        }
    }

    override suspend fun deleteAllUserRelatedFiles(userId: Int) {
        withContext(appDispatchers.io) {
            val datastoreFile = context.preferencesDataStoreFile("$USER_PREFERENCE_FILENAME-$userId")
            val providersFolder = context.getProvidersPathPrefix(userId)
            val providersSettingsFolder = context.getProvidersSettingsPathPrefix(userId)

            rmrf(File(providersFolder))
            rmrf(File(providersSettingsFolder))
            datastoreFile.delete()
        }
    }
}
