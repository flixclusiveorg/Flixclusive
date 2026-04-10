package com.flixclusive.data.backup.create.impl

import com.flixclusive.core.database.dao.provider.InstalledRepositoryDao
import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.data.backup.create.BackupCreator
import com.flixclusive.data.backup.model.BackupProviderRepository
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import javax.inject.Inject

internal class RepositoryBackupCreator @Inject constructor(
    private val installedRepositoryDao: InstalledRepositoryDao,
    private val userSessionDataStore: UserSessionDataStore,
) : BackupCreator<BackupProviderRepository> {
    override suspend fun invoke(): Result<List<BackupProviderRepository>> {
        return runCatching {
            val userId = userSessionDataStore.currentUserId.filterNotNull().first()
            val repositories = installedRepositoryDao.getAll(userId)

            repositories.map { repository ->
                BackupProviderRepository(
                    url = repository.url,
                    name = repository.name,
                    owner = repository.owner,
                    rawLinkFormat = repository.rawLinkFormat,
                    createdAt = repository.createdAt.time,
                    updatedAt = repository.updatedAt.time,
                )
            }
        }
    }
}
