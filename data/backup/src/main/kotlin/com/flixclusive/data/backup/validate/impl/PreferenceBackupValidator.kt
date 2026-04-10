package com.flixclusive.data.backup.validate.impl

import android.content.Context
import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.data.backup.model.BackupPreference
import com.flixclusive.data.backup.util.BackupPreferenceUtil.getLocalPreferences
import com.flixclusive.data.backup.validate.BackupValidationMode
import com.flixclusive.data.backup.validate.BackupValidator
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import javax.inject.Inject

internal class PreferenceBackupValidator @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val userSessionDataStore: UserSessionDataStore,
) : BackupValidator<BackupPreference> {
    override suspend fun invoke(
        backup: List<BackupPreference>,
        mode: BackupValidationMode,
    ): Result<Set<String>> {
        return runCatching {
            val userId = userSessionDataStore.currentUserId.filterNotNull().first()

            when (mode) {
                BackupValidationMode.CREATE -> context.validateCreate(userId = userId, backup = backup)
                BackupValidationMode.RESTORE -> context.validateRestore(userId = userId, backup = backup)
            }
        }
    }

    private suspend fun Context.validateCreate(userId: Int, backup: List<BackupPreference>): Set<String> {
        val expected = getLocalPreferences(userId)
        val actualByKey = backup.associateBy { it.key }

        val missing = linkedSetOf<String>()
        expected.forEach { (key, expectedValue) ->
            val actualValue = actualByKey[key]?.asStringOrNull
            if (actualValue != expectedValue) missing.add(key)
        }

        return missing
    }

    private suspend fun Context.validateRestore(userId: Int, backup: List<BackupPreference>): Set<String> {
        val expectedByKey = backup.associateBy { it.key }
        if (expectedByKey.isEmpty()) return emptySet()

        val actual = getLocalPreferences(userId)

        val missing = linkedSetOf<String>()
        expectedByKey.forEach { (key, expected) ->
            val expectedValue = expected.asStringOrNull
            val actualValue = actual[key]

            if (expectedValue == null || actualValue != expectedValue) missing.add(key)
        }

        return missing
    }
}
