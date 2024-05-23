package com.flixclusive.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.flixclusive.model.database.WatchHistoryItem
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchHistoryDao {
    @Transaction
    @Query("""
        SELECT wh.* 
        FROM watch_history AS wh
        JOIN User AS u 
        ON wh.ownerId = u.userId 
        WHERE u.userId = :ownerId
        ORDER BY wh.dateWatched DESC;
    """)
    fun getAllItemsInFlow(ownerId: Int): Flow<List<WatchHistoryItem>>

    @Transaction
    @Query("""
        SELECT wh.* 
        FROM watch_history AS wh
        JOIN User AS u 
        ON wh.ownerId = u.userId
        WHERE userId = :ownerId
        ORDER BY RANDOM() LIMIT :count
    """)
    suspend fun getRandomItems(ownerId: Int, count: Int): List<WatchHistoryItem>

    @Query("SELECT * FROM watch_history WHERE id = :itemId AND ownerId = :ownerId")
    suspend fun getWatchHistoryItemById(itemId: Int, ownerId: Int): WatchHistoryItem?

    @Query("SELECT * FROM watch_history WHERE id = :itemId AND ownerId = :ownerId")
    fun getWatchHistoryItemByIdInFlow(itemId: Int, ownerId: Int): Flow<WatchHistoryItem?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: WatchHistoryItem)

    @Query("DELETE FROM watch_history WHERE id = :itemId AND ownerId = :ownerId")
    suspend fun deleteById(itemId: Int, ownerId: Int)
}
