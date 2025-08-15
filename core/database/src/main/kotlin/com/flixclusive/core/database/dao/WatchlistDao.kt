package com.flixclusive.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.flixclusive.core.database.entity.WatchlistItem
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchlistDao {
    @Insert
    suspend fun insert(item: WatchlistItem)

    @Delete
    suspend fun delete(item: WatchlistItem)

    @Query("DELETE FROM watchlist WHERE ownerId = :ownerId")
    suspend fun deleteAll(ownerId: Int)

    @Query("DELETE FROM watchlist WHERE id = :itemId AND ownerId = :ownerId")
    suspend fun deleteById(itemId: String, ownerId: Int)

    @Query("SELECT * FROM watchlist WHERE id = :itemId AND ownerId = :ownerId")
    suspend fun getWatchlistItemById(itemId: String, ownerId: Int): WatchlistItem?

    @Transaction
    @Query("""
        SELECT w.*
        FROM watchlist as w
        JOIN User as u
        ON w.ownerId = u.userId
        WHERE userId = :ownerId
        ORDER BY w.addedOn DESC;
    """)
    suspend fun getAllItems(ownerId: Int): List<WatchlistItem>

    @Transaction
    @Query("""
        SELECT w.*
        FROM watchlist as w
        JOIN User as u
        ON w.ownerId = u.userId
        WHERE userId = :ownerId
        ORDER BY w.addedOn DESC;
    """)
    fun getAllItemsInFlow(ownerId: Int): Flow<List<WatchlistItem>>
}
