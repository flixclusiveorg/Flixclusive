package com.flixclusive.data.backup.restore.impl

import com.flixclusive.core.database.dao.provider.InstalledRepositoryDao
import com.flixclusive.core.database.entity.provider.InstalledRepository
import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.data.backup.model.BackupProviderRepository
import com.flixclusive.data.backup.restore.BackupRestorer
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import java.util.Date
import javax.inject.Inject

internal class RepositoryBackupRestorer @Inject constructor(
    private val installedRepositoryDao: InstalledRepositoryDao,
    private val userSessionDataStore: UserSessionDataStore,
) : BackupRestorer<BackupProviderRepository> {
    override suspend fun invoke(items: List<BackupProviderRepository>): Result<Unit> {
        return runCatching {
            val userId = userSessionDataStore.currentUserId.filterNotNull().first()

            installedRepositoryDao.deleteAll(userId)

            items.forEach { repository ->
                installedRepositoryDao.insert(
                    InstalledRepository(
                        url = repository.url,
                        userId = userId,
                        owner = repository.owner,
                        name = repository.name,
                        rawLinkFormat = repository.rawLinkFormat,
                        createdAt = Date(repository.createdAt),
                        updatedAt = Date(repository.updatedAt),
                    )
                )
            }
        }
    }
}
