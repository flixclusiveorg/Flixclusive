package com.flixclusive.data.backup.create.impl

import android.content.Context
import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.data.backup.create.BackupCreator
import com.flixclusive.data.backup.model.BackupPreference
import com.flixclusive.data.backup.model.StringPreferenceValue
import com.flixclusive.data.backup.util.BackupPreferenceUtil.getLocalPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import javax.inject.Inject

internal class PreferenceBackupCreator @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val userSessionDataStore: UserSessionDataStore,
) : BackupCreator<BackupPreference> {
    override suspend fun invoke(): Result<List<BackupPreference>> {
        return runCatching {
            val userId = userSessionDataStore.currentUserId.filterNotNull().first()
            val localPrefs = context.getLocalPreferences(userId)

            localPrefs.toBackupPreferences()
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun Map<String, *>.toBackupPreferences(): List<BackupPreference> {
        return mapNotNull { (key, value) ->
            when (value) {
                is String -> BackupPreference(key, StringPreferenceValue(value))
                else -> throw IllegalArgumentException("Unsupported preference type for key: $key, value: $value")
            }
        }
    }
}
