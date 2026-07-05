package com.flixclusive.data.backup.validate.impl

import com.flixclusive.core.database.dao.provider.CachedMediaLinkDao
import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.data.backup.model.BackupCachedLink
import com.flixclusive.data.backup.validate.BackupValidationMode
import com.flixclusive.data.backup.validate.BackupValidator
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import javax.inject.Inject

internal class CachedLinkBackupValidator @Inject constructor(
    private val cachedMediaLinkDao: CachedMediaLinkDao,
    private val userSessionDataStore: UserSessionDataStore,
) : BackupValidator<BackupCachedLink> {
    override suspend fun invoke(
        backup: List<BackupCachedLink>,
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

    private suspend fun validateCreate(ownerId: String, backup: List<BackupCachedLink>): Set<String> {
        val expected = cachedMediaLinkDao.getStreamsByOwner(ownerId).map { it.url }.toSet() +
            cachedMediaLinkDao.getSubtitlesByOwner(ownerId).map { it.url }.toSet()

        val actual = backup.map { it.url }.toSet()

        return expected.filter { it !in actual }.toSet()
    }

    private suspend fun validateRestore(ownerId: String, backup: List<BackupCachedLink>): Set<String> {
        if (backup.isEmpty()) return emptySet()

        val actual = cachedMediaLinkDao.getStreamsByOwner(ownerId).map { it.url }.toSet() +
            cachedMediaLinkDao.getSubtitlesByOwner(ownerId).map { it.url }.toSet()

        val expected = backup.map { it.url }.toSet()

        return expected.filter { it !in actual }.toSet()
    }
}
