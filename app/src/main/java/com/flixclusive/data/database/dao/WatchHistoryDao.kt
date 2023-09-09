package com.flixclusive.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.flixclusive.domain.model.entities.UserWithWatchHistoryList
import com.flixclusive.domain.model.entities.WatchHistoryItem
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchHistoryDao {
    @Transaction
    @Query("SELECT * FROM User where userId = :ownerId IN (SELECT DISTINCT ownerId FROM watch_history)")
    fun getAllItemsInFlow(ownerId: Int): Flow<UserWithWatchHistoryList?>

    @Transaction
    @Query("SELECT * FROM User where userId = :ownerId IN (SELECT DISTINCT ownerId FROM watch_history ORDER BY RANDOM() LIMIT :count)")
    suspend fun getRandomItems(ownerId: Int, count: Int): UserWithWatchHistoryList?

    @Query("SELECT * FROM watch_history WHERE id = :itemId AND ownerId = :ownerId")
    suspend fun getWatchHistoryItemById(itemId: Int, ownerId: Int): WatchHistoryItem?

    @Query("SELECT * FROM watch_history WHERE id = :itemId AND ownerId = :ownerId")
    fun getWatchHistoryItemByIdInFlow(itemId: Int, ownerId: Int): Flow<WatchHistoryItem?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: WatchHistoryItem)

    @Query("DELETE FROM watch_history WHERE id = :itemId AND ownerId = :ownerId")
    suspend fun deleteById(itemId: Int, ownerId: Int)
}
