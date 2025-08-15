package com.flixclusive.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.flixclusive.core.database.entity.WatchHistory
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
    fun getAllItemsInFlow(ownerId: Int): Flow<List<WatchHistory>>

    @Transaction
    @Query("""
        SELECT wh.*
        FROM watch_history AS wh
        JOIN User AS u
        ON wh.ownerId = u.userId
        WHERE userId = :ownerId
        ORDER BY RANDOM() LIMIT :count
    """)
    fun getRandomItems(ownerId: Int, count: Int): Flow<List<WatchHistory>>

    @Query("SELECT * FROM watch_history WHERE id = :itemId AND ownerId = :ownerId")
    suspend fun getWatchHistoryItemById(itemId: String, ownerId: Int): WatchHistory?

    @Query("SELECT * FROM watch_history WHERE id = :itemId AND ownerId = :ownerId")
    fun getWatchHistoryItemByIdInFlow(itemId: String, ownerId: Int): Flow<WatchHistory?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: WatchHistory)

    @Query("DELETE FROM watch_history WHERE id = :itemId AND ownerId = :ownerId")
    suspend fun deleteById(itemId: String, ownerId: Int)

    @Query("DELETE FROM watch_history WHERE ownerId = :ownerId")
    suspend fun deleteAll(ownerId: Int)
}
