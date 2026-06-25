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
    @Query("SELECT * FROM installed_providers ORDER BY createdAt ASC")
    suspend fun getAll(): List<InstalledProvider>

    @Query("SELECT * FROM installed_providers WHERE ownerId = :ownerId ORDER BY createdAt ASC")
    fun getAllAsFlow(ownerId: String): Flow<List<InstalledProvider>>

    @Query("SELECT * FROM installed_providers WHERE ownerId = :ownerId ORDER BY createdAt ASC")
    suspend fun getAll(ownerId: String): List<InstalledProvider>

    @Query("SELECT * FROM installed_providers WHERE id = :id AND ownerId = :ownerId")
    suspend fun get(id: String, ownerId: String): InstalledProvider?

    @Query("SELECT * FROM installed_providers WHERE id = :id AND ownerId = :ownerId")
    fun getAsFlow(id: String, ownerId: String): Flow<InstalledProvider?>

    @Query(
        "SELECT * FROM installed_providers WHERE repositoryUrl = :repositoryUrl AND ownerId = :ownerId ORDER BY createdAt ASC"
    )
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

    @Query(
        "UPDATE installed_providers SET isCatalogEnabled = :isEnabled, updatedAt = :updatedAt WHERE id = :id AND ownerId = :ownerId"
    )
    suspend fun setCatalogEnabled(
        id: String,
        ownerId: String,
        isEnabled: Boolean,
        updatedAt: Long = System.currentTimeMillis(),
    )

    @Query(
        "UPDATE installed_providers SET isCrossMatchEnabled = :isEnabled, updatedAt = :updatedAt WHERE id = :id AND ownerId = :ownerId"
    )
    suspend fun setCrossMatchEnabled(
        id: String,
        ownerId: String,
        isEnabled: Boolean,
        updatedAt: Long = System.currentTimeMillis(),
    )

    @Query(
        "UPDATE installed_providers SET isMediaLinkEnabled = :isEnabled, updatedAt = :updatedAt WHERE id = :id AND ownerId = :ownerId"
    )
    suspend fun setMediaLinkEnabled(
        id: String,
        ownerId: String,
        isEnabled: Boolean,
        updatedAt: Long = System.currentTimeMillis(),
    )

    @Query(
        "UPDATE installed_providers SET isMetadataEnabled = :isEnabled, updatedAt = :updatedAt WHERE id = :id AND ownerId = :ownerId"
    )
    suspend fun setMetadataEnabled(
        id: String,
        ownerId: String,
        isEnabled: Boolean,
        updatedAt: Long = System.currentTimeMillis(),
    )

    @Query(
        "UPDATE installed_providers SET isSearchEnabled = :isEnabled, updatedAt = :updatedAt WHERE id = :id AND ownerId = :ownerId"
    )
    suspend fun setSearchEnabled(
        id: String,
        ownerId: String,
        isEnabled: Boolean,
        updatedAt: Long = System.currentTimeMillis(),
    )

    @Query(
        "UPDATE installed_providers SET isTrackerEnabled = :isEnabled, updatedAt = :updatedAt WHERE id = :id AND ownerId = :ownerId"
    )
    suspend fun setTrackerEnabled(
        id: String,
        ownerId: String,
        isEnabled: Boolean,
        updatedAt: Long = System.currentTimeMillis(),
    )
}
