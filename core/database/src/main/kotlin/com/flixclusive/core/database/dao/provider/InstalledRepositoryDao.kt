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
    @Query("SELECT * FROM repositories")
    fun getAllAsFlow(): Flow<List<InstalledRepository>>

    @Query("SELECT * FROM repositories WHERE url = :url")
    suspend fun get(url: String): InstalledRepository?

    @Query("SELECT * FROM repositories WHERE url = :url")
    fun getAsFlow(url: String): Flow<InstalledRepository?>

    @Upsert
    suspend fun insert(repository: InstalledRepository)

    @Upsert
    suspend fun insert(repositories: List<InstalledRepository>)

    @Update
    suspend fun update(repository: InstalledRepository)

    @Query("DELETE FROM repositories WHERE url = :url")
    suspend fun delete(url: String)

    @Query("DELETE FROM repositories")
    suspend fun deleteAll()
}
