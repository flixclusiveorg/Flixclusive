package com.flixclusive.data.database.watch_history

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.flixclusive.domain.model.entities.WatchHistoryItem
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchHistoryDao {
    @Query("SELECT * FROM watch_history ORDER BY dateWatched DESC LIMIT 1")
    suspend fun getLatestItem(): WatchHistoryItem?

    @Query("SELECT * FROM watch_history ORDER BY dateWatched DESC")
    suspend fun getAllItems(): List<WatchHistoryItem>

    @Query("SELECT * FROM watch_history ORDER BY dateWatched DESC")
    fun getAllItemsInFlow(): Flow<List<WatchHistoryItem>>

    @Query("SELECT * FROM watch_history WHERE id = :itemId")
    suspend fun getWatchHistoryItemById(itemId: Int): WatchHistoryItem?

    @Query("SELECT * FROM watch_history WHERE id = :itemId")
    fun getWatchHistoryItemByIdInFlow(itemId: Int): Flow<WatchHistoryItem?>

    @Query("SELECT * FROM watch_history ORDER BY RANDOM() LIMIT :count")
    fun getRandomWatchHistoryItems(count: Int): List<WatchHistoryItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: WatchHistoryItem)

    @Query("DELETE FROM watch_history WHERE id = :itemId")
    suspend fun deleteById(itemId: Int)
}
