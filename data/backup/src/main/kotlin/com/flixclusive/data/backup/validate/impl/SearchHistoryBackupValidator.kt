package com.flixclusive.data.backup.validate.impl

import com.flixclusive.core.database.dao.SearchHistoryDao
import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.data.backup.model.BackupSearchHistory
import com.flixclusive.data.backup.validate.BackupValidationMode
import com.flixclusive.data.backup.validate.BackupValidator
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import javax.inject.Inject

internal class SearchHistoryBackupValidator @Inject constructor(
    private val searchHistoryDao: SearchHistoryDao,
    private val userSessionDataStore: UserSessionDataStore,
) : BackupValidator<BackupSearchHistory> {
    override suspend fun invoke(
        backup: List<BackupSearchHistory>,
        mode: BackupValidationMode,
    ): Result<Set<String>> {
        return runCatching {
            val ownerId = userSessionDataStore.currentUserId.filterNotNull().first()

            when (mode) {
                BackupValidationMode.CREATE -> validateCreate(ownerId = ownerId, backup = backup)
                BackupValidationMode.RESTORE -> validateRestore(ownerId = ownerId, backup = backup)
            }
        }
    }

    private suspend fun validateCreate(ownerId: String, backup: List<BackupSearchHistory>): Set<String> {
        val expectedHistory = searchHistoryDao.getAll(ownerId)
        val backupByQuery = backup.associateBy { it.query }

        val missing = linkedSetOf<String>()
        expectedHistory.forEach { expected ->
            val actual = backupByQuery[expected.query]
            if (actual == null) {
                missing.add(expected.query)
                return@forEach
            }

            val matches = actual.createdAt == expected.createdAt.time &&
                actual.updatedAt == expected.updatedAt.time

            if (!matches) missing.add(expected.query)
        }

        return missing
    }

    private suspend fun validateRestore(ownerId: String, backup: List<BackupSearchHistory>): Set<String> {
        val expectedByQuery = backup.associateBy { it.query }
        if (expectedByQuery.isEmpty()) return emptySet()

        val actualByQuery = searchHistoryDao.getAll(ownerId).associateBy { it.query }

        val missing = linkedSetOf<String>()
        expectedByQuery.forEach { (query, expected) ->
            val actual = actualByQuery[query]
            if (actual == null) {
                missing.add(query)
                return@forEach
            }

            val matches = expected.createdAt == actual.createdAt.time &&
                expected.updatedAt == actual.updatedAt.time

            if (!matches) missing.add(query)
        }

        return missing
    }
}
