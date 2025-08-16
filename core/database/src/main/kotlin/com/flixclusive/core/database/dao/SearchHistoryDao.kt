package com.flixclusive.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.flixclusive.core.database.entity.search.SearchHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface SearchHistoryDao {
    @Query("SELECT * FROM search_history WHERE ownerId = :ownerId ORDER BY searchedOn DESC")
    fun getAll(ownerId: Int): Flow<List<SearchHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: SearchHistory): Long

    @Query("DELETE FROM search_history WHERE id = :id")
    suspend fun delete(id: Int)

    @Query("DELETE FROM search_history WHERE ownerId = :ownerId")
    suspend fun deleteAll(ownerId: Int)
}
