package com.flixclusive.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.flixclusive.model.database.SearchHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface SearchHistoryDao {
    @Query("SELECT * FROM search_history WHERE ownerId = :ownerId ORDER BY searchedOn DESC")
    fun getSearchHistory(ownerId: Int): Flow<List<SearchHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: SearchHistory)

    @Query("DELETE FROM search_history WHERE id = :id AND ownerId = :ownerId")
    suspend fun deleteById(ownerId: Int, id: Int)
}