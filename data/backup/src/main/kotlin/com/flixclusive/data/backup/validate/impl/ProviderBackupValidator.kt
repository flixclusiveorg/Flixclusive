package com.flixclusive.data.backup.validate.impl

import com.flixclusive.core.database.dao.provider.InstalledProviderDao
import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.data.backup.model.BackupProvider
import com.flixclusive.data.backup.validate.BackupValidationMode
import com.flixclusive.data.backup.validate.BackupValidator
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import javax.inject.Inject

internal class ProviderBackupValidator @Inject constructor(
    private val installedProviderDao: InstalledProviderDao,
    private val userSessionDataStore: UserSessionDataStore,
) : BackupValidator<BackupProvider> {
    override suspend fun invoke(
        backup: List<BackupProvider>,
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

    private suspend fun validateCreate(ownerId: Int, backup: List<BackupProvider>): Set<String> {
        val expectedProviders = installedProviderDao.getAll(ownerId)
        val backupById = backup.associateBy { it.id }

        val missing = linkedSetOf<String>()
        expectedProviders.forEach { expected ->
            val actual = backupById[expected.id]
            if (actual == null) {
                missing.add(expected.id)
                return@forEach
            }

            val matches = actual.repositoryUrl == expected.repositoryUrl &&
                actual.sortOrder == expected.sortOrder &&
                actual.isEnabled == expected.isEnabled &&
                actual.createdAt == expected.createdAt.time &&
                actual.updatedAt == expected.updatedAt.time

            if (!matches) missing.add(expected.id)
        }

        return missing
    }

    private suspend fun validateRestore(ownerId: Int, backup: List<BackupProvider>): Set<String> {
        val expectedById = backup.associateBy { it.id }
        if (expectedById.isEmpty()) return emptySet()

        val actualProvidersById = installedProviderDao.getAll(ownerId).associateBy { it.id }

        val missing = linkedSetOf<String>()
        expectedById.forEach { (id, expected) ->
            val actual = actualProvidersById[id]
            if (actual == null) {
                missing.add(id)
                return@forEach
            }

            val matches = expected.repositoryUrl == actual.repositoryUrl &&
                expected.sortOrder == actual.sortOrder &&
                expected.isEnabled == actual.isEnabled &&
                expected.createdAt == actual.createdAt.time &&
                expected.updatedAt == actual.updatedAt.time

            if (!matches) missing.add(id)
        }

        return missing
    }
}
