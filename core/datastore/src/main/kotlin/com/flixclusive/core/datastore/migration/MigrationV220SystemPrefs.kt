package com.flixclusive.core.datastore.migration

import androidx.datastore.core.DataMigration
import com.flixclusive.core.datastore.model.system.SystemPreferences

internal object MigrationV220SystemPrefs : DataMigration<SystemPreferences> {
    override suspend fun shouldMigrate(currentData: SystemPreferences): Boolean {
        return currentData.storageDirectoryUri == null
    }

    override suspend fun migrate(currentData: SystemPreferences): SystemPreferences {
        return currentData.copy(
            storageDirectoryUri = null,
            isFirstTimeUserLaunch = true,
        )
    }

    override suspend fun cleanUp() = Unit
}
