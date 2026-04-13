package com.flixclusive.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.flixclusive.core.database.entity.search.SearchHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface SearchHistoryDao {
    @Query("SELECT * FROM search_history WHERE ownerId = :ownerId ORDER BY updatedAt DESC")
    fun getAllAsFlow(ownerId: String): Flow<List<SearchHistory>>

    @Query("SELECT * FROM search_history WHERE ownerId = :ownerId ORDER BY updatedAt DESC")
    suspend fun getAll(ownerId: String): List<SearchHistory>

    @Upsert
    suspend fun insert(item: SearchHistory): Long

    @Query("DELETE FROM search_history WHERE id = :id")
    suspend fun delete(id: Int)

    @Query("DELETE FROM search_history WHERE ownerId = :ownerId")
    suspend fun deleteAll(ownerId: String)
}
