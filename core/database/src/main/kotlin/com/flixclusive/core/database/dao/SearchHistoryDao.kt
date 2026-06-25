package com.flixclusive.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.flixclusive.core.database.entity.search.SearchHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface SearchHistoryDao {
    @Query("SELECT * FROM search_history WHERE ownerId = :ownerId ORDER BY updatedAt DESC")
    fun getAllAsFlow(ownerId: String): Flow<List<SearchHistory>>

    @Query("SELECT * FROM search_history WHERE ownerId = :ownerId ORDER BY updatedAt DESC")
    suspend fun getAll(ownerId: String): List<SearchHistory>

    @Query("SELECT * FROM search_history WHERE ownerId = :ownerId AND query = :query")
    suspend fun get(ownerId: String, query: String): SearchHistory?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: SearchHistory): Long

    @Query("DELETE FROM search_history WHERE id = :id")
    suspend fun delete(id: Int)

    @Query("DELETE FROM search_history WHERE ownerId = :ownerId")
    suspend fun deleteAll(ownerId: String)
}
