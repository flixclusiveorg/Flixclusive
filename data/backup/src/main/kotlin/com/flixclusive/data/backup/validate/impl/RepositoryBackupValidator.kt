package com.flixclusive.data.backup.validate.impl

import com.flixclusive.core.database.dao.provider.InstalledRepositoryDao
import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.data.backup.model.BackupProviderRepository
import com.flixclusive.data.backup.validate.BackupValidationMode
import com.flixclusive.data.backup.validate.BackupValidator
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import javax.inject.Inject

internal class RepositoryBackupValidator @Inject constructor(
    private val installedRepositoryDao: InstalledRepositoryDao,
    private val userSessionDataStore: UserSessionDataStore,
) : BackupValidator<BackupProviderRepository> {
    override suspend fun invoke(
        backup: List<BackupProviderRepository>,
        mode: BackupValidationMode,
    ): Result<Set<String>> {
        return runCatching {
            val userId = userSessionDataStore.currentUserId.filterNotNull().first()

            when (mode) {
                BackupValidationMode.CREATE -> validateCreate(userId = userId, backup = backup)
                BackupValidationMode.RESTORE -> validateRestore(userId = userId, backup = backup)
            }
        }
    }

    private suspend fun validateCreate(userId: Int, backup: List<BackupProviderRepository>): Set<String> {
        val expectedRepositories = installedRepositoryDao.getAll(userId)
        val backupByUrl = backup.associateBy { it.url }

        val missing = linkedSetOf<String>()
        expectedRepositories.forEach { expected ->
            val actual = backupByUrl[expected.url]
            if (actual == null) {
                missing.add(expected.url)
                return@forEach
            }

            val matches = actual.owner == expected.owner &&
                actual.name == expected.name &&
                actual.rawLinkFormat == expected.rawLinkFormat &&
                actual.createdAt == expected.createdAt.time &&
                actual.updatedAt == expected.updatedAt.time

            if (!matches) missing.add(expected.url)
        }

        return missing
    }

    private suspend fun validateRestore(userId: Int, backup: List<BackupProviderRepository>): Set<String> {
        val expectedByUrl = backup.associateBy { it.url }
        if (expectedByUrl.isEmpty()) return emptySet()

        val actualByUrl = installedRepositoryDao.getAll(userId).associateBy { it.url }

        val missing = linkedSetOf<String>()
        expectedByUrl.forEach { (url, expected) ->
            val actual = actualByUrl[url]
            if (actual == null) {
                missing.add(url)
                return@forEach
            }

            val matches = expected.owner == actual.owner &&
                expected.name == actual.name &&
                expected.rawLinkFormat == actual.rawLinkFormat &&
                expected.createdAt == actual.createdAt.time &&
                expected.updatedAt == actual.updatedAt.time

            if (!matches) missing.add(url)
        }

        return missing
    }
}
