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
        TODO("Not yet implemented")
    }

    override suspend fun insert(installedRepository: InstalledRepository) {
        withContext(appDispatchers.io) {
            repositoryDao.insert(installedRepository)
        }
    }

    override suspend fun delete(installedRepository: InstalledRepository) {
        withContext(appDispatchers.io) {
            repositoryDao.delete(installedRepository.url, installedRepository.userId)
        }
    }
}
