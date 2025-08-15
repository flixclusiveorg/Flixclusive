package com.flixclusive.core.datastore.migration

import android.content.Context
import androidx.datastore.core.DataMigration
import androidx.datastore.dataStoreFile
import com.flixclusive.core.datastore.SYSTEM_PREFS_FILENAME
import com.flixclusive.core.datastore.migration.model.OldAppSettings
import com.flixclusive.core.datastore.model.system.SystemPreferences
import kotlinx.coroutines.flow.first

internal class SystemPreferencesMigration(private val context: Context) : DataMigration<SystemPreferences> {
    private val oldDataStoreFile
        = context.dataStoreFile(OLD_APP_SETTINGS_FILENAME)
    private val newDataStoreFile
        = context.dataStoreFile(SYSTEM_PREFS_FILENAME)

    override suspend fun cleanUp() = Unit

    override suspend fun migrate(currentData: SystemPreferences): SystemPreferences {
        val oldData = context.oldAppSettings.data.first()
        val oldOnBoardingPreferences = context.oldOnBoardingPreferences.data.first()

        return oldData.toSystemPreferences()
            .copy(lastSeenChangelogs = oldOnBoardingPreferences.lastSeenChangelogsVersion)
    }

    override suspend fun shouldMigrate(currentData: SystemPreferences): Boolean {
        return oldDataStoreFile.exists() && !newDataStoreFile.exists()
    }

    private fun OldAppSettings.toSystemPreferences(): SystemPreferences {
        return SystemPreferences(
            isUsingAutoUpdateAppFeature = isUsingAutoUpdateAppFeature,
            isUsingPrereleaseUpdates = isUsingPrereleaseUpdates,
            isSendingCrashLogsAutomatically = isSendingCrashLogsAutomatically,
            userAgent = userAgent,
            dns = dns,
        )
    }
}
