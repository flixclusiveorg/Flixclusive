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
    @Query("SELECT * FROM installed_providers ORDER BY sortOrder ASC")
    suspend fun getAll(): List<InstalledProvider>

    @Query("SELECT * FROM installed_providers WHERE ownerId = :ownerId ORDER BY sortOrder ASC")
    fun getAllAsFlow(ownerId: String): Flow<List<InstalledProvider>>

    @Query("SELECT * FROM installed_providers WHERE ownerId = :ownerId ORDER BY sortOrder ASC")
    suspend fun getAll(ownerId: String): List<InstalledProvider>

    @Query("SELECT * FROM installed_providers WHERE isEnabled = 1 AND ownerId = :ownerId ORDER BY sortOrder ASC")
    fun getEnabledAsFlow(ownerId: String): Flow<List<InstalledProvider>>

    @Query("SELECT * FROM installed_providers WHERE isEnabled = 1 AND ownerId = :ownerId ORDER BY sortOrder ASC")
    suspend fun getEnabled(ownerId: String): List<InstalledProvider>

    @Query("SELECT * FROM installed_providers WHERE id = :id AND ownerId = :ownerId")
    suspend fun get(id: String, ownerId: String): InstalledProvider?

    @Query("SELECT * FROM installed_providers WHERE id = :id AND ownerId = :ownerId")
    fun getAsFlow(id: String, ownerId: String): Flow<InstalledProvider?>

    @Query("SELECT * FROM installed_providers WHERE repositoryUrl = :repositoryUrl AND ownerId = :ownerId ORDER BY sortOrder ASC")
    fun getByRepositoryUrl(repositoryUrl: String, ownerId: String): Flow<List<InstalledProvider>>

    @Upsert
    suspend fun insert(provider: InstalledProvider)

    @Upsert
    suspend fun insert(providers: List<InstalledProvider>)

    @Update
    suspend fun update(provider: InstalledProvider)

    @Delete
    suspend fun delete(provider: InstalledProvider)

    @Query("DELETE FROM installed_providers WHERE id = :id AND ownerId = :ownerId")
    suspend fun delete(id: String, ownerId: String)

    @Query("DELETE FROM installed_providers WHERE ownerId = :ownerId")
    suspend fun deleteAll(ownerId: String)

    @Query("UPDATE installed_providers SET sortOrder = :sortOrder, updatedAt = :updatedAt WHERE id = :id AND ownerId = :ownerId")
    suspend fun updateSortOrder(
        id: String,
        ownerId: String,
        sortOrder: Double,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("UPDATE installed_providers SET isEnabled = :isEnabled, updatedAt = :updatedAt WHERE id = :id AND ownerId = :ownerId")
    suspend fun setEnabled(
        id: String,
        ownerId: String,
        isEnabled: Boolean,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("SELECT isEnabled FROM installed_providers WHERE id = :id AND ownerId = :ownerId")
    suspend fun isEnabled(id: String, ownerId: String): Boolean

    @Query("SELECT MAX(sortOrder) FROM installed_providers WHERE ownerId = :ownerId")
    suspend fun getMaxSortOrder(ownerId: String): Double?
}
