package com.flixclusive.core.database.dao.provider

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.flixclusive.core.database.entity.provider.InstalledRepository
import kotlinx.coroutines.flow.Flow

@Dao
interface InstalledRepositoryDao {
    @Query("SELECT * FROM repositories WHERE userId = :userId ORDER BY createdAt DESC")
    fun getAllAsFlow(userId: Int): Flow<List<InstalledRepository>>

    @Query("SELECT * FROM repositories WHERE userId = :userId ORDER BY createdAt DESC")
    suspend fun getAll(userId: Int): List<InstalledRepository>

    @Query("SELECT * FROM repositories WHERE url = :url AND userId = :userId")
    suspend fun get(url: String, userId: Int): InstalledRepository?

    @Query("SELECT * FROM repositories WHERE url = :url AND userId = :userId")
    fun getAsFlow(url: String, userId: Int): Flow<InstalledRepository?>

    @Query("SELECT EXISTS(SELECT 1 FROM repositories WHERE url = :url AND userId = :userId)")
    suspend fun isInstalled(url: String, userId: Int): Boolean

    @Upsert
    suspend fun insert(repository: InstalledRepository)

    @Upsert
    suspend fun insert(repositories: List<InstalledRepository>)

    @Update
    suspend fun update(repository: InstalledRepository)

    @Query("DELETE FROM repositories WHERE url = :url AND userId = :userId")
    suspend fun delete(url: String, userId: Int)

    @Query("DELETE FROM repositories WHERE userId = :userId")
    suspend fun deleteAll(userId: Int)
}
