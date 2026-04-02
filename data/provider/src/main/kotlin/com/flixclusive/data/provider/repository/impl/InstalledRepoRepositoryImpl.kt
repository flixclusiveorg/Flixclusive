package com.flixclusive.data.provider.repository.impl

import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.database.dao.provider.InstalledRepositoryDao
import com.flixclusive.core.database.entity.provider.InstalledRepository
import com.flixclusive.data.provider.repository.InstalledRepoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject

internal class InstalledRepoRepositoryImpl @Inject constructor(
    private val repositoryDao: InstalledRepositoryDao,
    private val appDispatchers: AppDispatchers
) : InstalledRepoRepository {
    override suspend fun getAll(ownerId: Int): List<InstalledRepository> {
        return withContext(appDispatchers.io) {
            repositoryDao.getAll(ownerId)
        }
    }

    override fun getAllAsFlow(ownerId: Int): Flow<List<InstalledRepository>> {
        return repositoryDao.getAllAsFlow(ownerId)
    }

    override suspend fun isInstalled(url: String, ownerId: Int): Boolean {
        return withContext(appDispatchers.io) {
            repositoryDao.isInstalled(url, ownerId)
        }
    }

    override suspend fun insert(item: InstalledRepository) {
        withContext(appDispatchers.io) {
            repositoryDao.insert(item)
        }
    }

    override suspend fun delete(item: InstalledRepository) {
        withContext(appDispatchers.io) {
            repositoryDao.delete(item.url, item.userId)
        }
    }

    override suspend fun deleteAll(ownerId: Int) {
        withContext(appDispatchers.io) {
            repositoryDao.deleteAll(ownerId)
        }
    }
}
