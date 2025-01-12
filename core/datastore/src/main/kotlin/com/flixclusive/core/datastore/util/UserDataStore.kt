package com.flixclusive.core.datastore.util

import android.content.Context
import androidx.annotation.GuardedBy
import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.flixclusive.core.datastore.util.DataStoreLock.USER_PREFS_INSTANCE
import com.flixclusive.core.util.coroutines.AppDispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

private const val USER_PREFERENCE_FILENAME = "users/user-preferences"

fun Context.createUserPreferences(
    userId: Int,
    corruptionHandler: ReplaceFileCorruptionHandler<Preferences>? = null,
    produceMigrations: (Context) -> List<DataMigration<Preferences>> = { listOf() },
    scope: CoroutineScope = CoroutineScope(AppDispatchers.IO.dispatcher + SupervisorJob()),
): DataStore<Preferences> {
    synchronized(DataStoreLock.lock) {
        if (DataStoreLock.CURRENT_USER_ID != userId) {
            DataStoreLock.CURRENT_SCOPE_USED?.cancel()

            USER_PREFS_INSTANCE = null
            DataStoreLock.CURRENT_USER_ID = userId
            DataStoreLock.CURRENT_SCOPE_USED = scope
        }
    }

    return USER_PREFS_INSTANCE ?: synchronized(DataStoreLock.lock) {
        if (USER_PREFS_INSTANCE == null) {
            USER_PREFS_INSTANCE =
                PreferenceDataStoreFactory.create(
                    corruptionHandler = corruptionHandler,
                    migrations = produceMigrations(applicationContext),
                    scope = scope,
                ) {
                    applicationContext.preferencesDataStoreFile("$USER_PREFERENCE_FILENAME-$userId")
                }
        }

        USER_PREFS_INSTANCE!!
    }
}

@Suppress("unused", "ktlint:standard:property-naming")
private object DataStoreLock {
    val lock = Any()

    @GuardedBy("lock")
    @Volatile
    var USER_PREFS_INSTANCE: DataStore<Preferences>? = null

    @GuardedBy("lock")
    @Volatile
    var CURRENT_USER_ID: Int? = null

    @GuardedBy("lock")
    @Volatile
    var CURRENT_SCOPE_USED: CoroutineScope? = null
}
