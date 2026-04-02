package com.flixclusive.data.provider.repository

import com.flixclusive.core.database.entity.provider.InstalledRepository
import kotlinx.coroutines.flow.Flow

interface InstalledRepoRepository {
    suspend fun getAll(ownerId: Int): List<InstalledRepository>

    fun getAllAsFlow(ownerId: Int): Flow<List<InstalledRepository>>

    suspend fun isInstalled(url: String, ownerId: Int): Boolean

    suspend fun insert(item: InstalledRepository)

    suspend fun delete(item: InstalledRepository)

    suspend fun deleteAll(ownerId: Int)
}
