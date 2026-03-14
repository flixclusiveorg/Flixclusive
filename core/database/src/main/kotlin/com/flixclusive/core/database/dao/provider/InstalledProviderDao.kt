package com.flixclusive.core.database.dao.provider

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.flixclusive.core.database.entity.provider.InstalledProvider
import kotlinx.coroutines.flow.Flow

@Dao
interface InstalledProviderDao {
    @Query("SELECT * FROM installed_providers ORDER BY sortOrder ASC")
    fun getAllOrderedBySortOrder(): Flow<List<InstalledProvider>>

    @Query("SELECT * FROM installed_providers WHERE isDisabled = 0 ORDER BY sortOrder ASC")
    fun getEnabled(): Flow<List<InstalledProvider>>

    @Query("SELECT * FROM installed_providers WHERE id = :id")
    suspend fun get(id: String): InstalledProvider?

    @Query("SELECT * FROM installed_providers WHERE id = :id")
    fun getAsFlow(id: String): Flow<InstalledProvider?>

    @Query("SELECT * FROM installed_providers WHERE repositoryUrl = :repositoryUrl ORDER BY sortOrder ASC")
    fun getByRepositoryUrl(repositoryUrl: String): Flow<List<InstalledProvider>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(provider: InstalledProvider)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(providers: List<InstalledProvider>)

    @Update
    suspend fun update(provider: InstalledProvider)

    @Query("DELETE FROM installed_providers WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM installed_providers")
    suspend fun deleteAll()

    @Query("UPDATE installed_providers SET sortOrder = :sortOrder, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateSortOrder(id: String, sortOrder: Double, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE installed_providers SET isDisabled = :isDisabled, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setDisabled(id: String, isDisabled: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT MAX(sortOrder) FROM installed_providers")
    suspend fun getMaxSortOrder(): Double?
}
