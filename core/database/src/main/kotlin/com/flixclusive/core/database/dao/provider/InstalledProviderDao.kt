package com.flixclusive.core.database.dao.provider

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.flixclusive.core.database.entity.provider.InstalledProvider
import kotlinx.coroutines.flow.Flow

@Dao
interface InstalledProviderDao {
    @Query("SELECT * FROM installed_providers WHERE ownerId = :ownerId ORDER BY sortOrder ASC")
    fun getAllAsFlow(ownerId: Int): Flow<List<InstalledProvider>>

    @Query("SELECT * FROM installed_providers WHERE ownerId = :ownerId ORDER BY sortOrder ASC")
    suspend fun getAll(ownerId: Int): List<InstalledProvider>

    @Query("SELECT * FROM installed_providers WHERE isEnabled = 1 AND ownerId = :ownerId ORDER BY sortOrder ASC")
    fun getEnabledAsFlow(ownerId: Int): Flow<List<InstalledProvider>>

    @Query("SELECT * FROM installed_providers WHERE isEnabled = 1 AND ownerId = :ownerId ORDER BY sortOrder ASC")
    suspend fun getEnabled(ownerId: Int): List<InstalledProvider>

    @Query("SELECT * FROM installed_providers WHERE id = :id AND ownerId = :ownerId")
    suspend fun get(id: String, ownerId: Int): InstalledProvider?

    @Query("SELECT * FROM installed_providers WHERE id = :id AND ownerId = :ownerId")
    fun getAsFlow(id: String, ownerId: Int): Flow<InstalledProvider?>

    @Query("SELECT * FROM installed_providers WHERE repositoryUrl = :repositoryUrl AND ownerId = :ownerId ORDER BY sortOrder ASC")
    fun getByRepositoryUrl(repositoryUrl: String, ownerId: Int): Flow<List<InstalledProvider>>

    @Upsert
    suspend fun insert(provider: InstalledProvider)

    @Upsert
    suspend fun insert(providers: List<InstalledProvider>)

    @Update
    suspend fun update(provider: InstalledProvider)

    @Delete
    suspend fun delete(provider: InstalledProvider)

    @Query("DELETE FROM installed_providers WHERE id = :id AND ownerId = :ownerId")
    suspend fun delete(id: String, ownerId: Int)

    @Query("DELETE FROM installed_providers WHERE ownerId = :ownerId")
    suspend fun deleteAll(ownerId: Int)

    @Query("UPDATE installed_providers SET sortOrder = :sortOrder, updatedAt = :updatedAt WHERE id = :id AND ownerId = :ownerId")
    suspend fun updateSortOrder(
        id: String,
        ownerId: Int,
        sortOrder: Double,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("UPDATE installed_providers SET isEnabled = :isEnabled, updatedAt = :updatedAt WHERE id = :id AND ownerId = :ownerId")
    suspend fun setEnabled(
        id: String,
        ownerId: Int,
        isEnabled: Boolean,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("SELECT isEnabled FROM installed_providers WHERE id = :id AND ownerId = :ownerId")
    suspend fun isEnabled(id: String, ownerId: Int): Boolean

    @Query("SELECT MAX(sortOrder) FROM installed_providers WHERE ownerId = :ownerId")
    suspend fun getMaxSortOrder(ownerId: Int): Double?
}
