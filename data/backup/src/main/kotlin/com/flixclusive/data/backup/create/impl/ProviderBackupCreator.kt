package com.flixclusive.data.backup.create.impl

import com.flixclusive.core.database.dao.provider.InstalledProviderDao
import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.data.backup.create.BackupCreator
import com.flixclusive.data.backup.model.BackupProvider
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import javax.inject.Inject

internal class ProviderBackupCreator @Inject constructor(
    private val installedProviderDao: InstalledProviderDao,
    private val userSessionDataStore: UserSessionDataStore
) : BackupCreator<BackupProvider> {
    override suspend fun invoke(): Result<List<BackupProvider>> {
        return runCatching {
            val userId = userSessionDataStore.currentUserId.filterNotNull().first()
            val providers = installedProviderDao.getAll(userId)

            providers.map { provider ->
                BackupProvider(
                    id = provider.id,
                    repositoryUrl = provider.repositoryUrl,
                    sortOrder = provider.sortOrder,
                    isEnabled = provider.isEnabled,
                    createdAt = provider.createdAt.time,
                    updatedAt = provider.updatedAt.time,
                )
            }
        }
    }
}
