package com.flixclusive.data.provider.repository

import com.flixclusive.core.database.entity.provider.InstalledRepository
import kotlinx.coroutines.flow.Flow

interface InstalledRepoRepository {
    suspend fun getAll(ownerId: String): List<InstalledRepository>

    fun getAllAsFlow(ownerId: String): Flow<List<InstalledRepository>>

    suspend fun isInstalled(url: String, ownerId: String): Boolean

    suspend fun insert(item: InstalledRepository)

    suspend fun delete(item: InstalledRepository)

    suspend fun deleteAll(ownerId: String)
}
