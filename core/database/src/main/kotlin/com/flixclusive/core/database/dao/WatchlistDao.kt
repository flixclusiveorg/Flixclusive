package com.flixclusive.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.flixclusive.model.database.UserWithWatchlist
import com.flixclusive.model.database.WatchlistItem
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchlistDao {
    @Insert
    suspend fun insert(item: WatchlistItem)

    @Delete
    suspend fun delete(item: WatchlistItem)

    @Query("DELETE FROM watchlist WHERE id = :itemId AND ownerId = :ownerId")
    suspend fun deleteById(itemId: Int, ownerId: Int)

    @Query("SELECT * FROM watchlist WHERE id = :filmId AND ownerId = :ownerId")
    suspend fun getWatchlistItemById(filmId: Int, ownerId: Int): WatchlistItem?

    @Transaction
    @Query("SELECT * FROM User where userId = :ownerId")
    suspend fun getAllItems(ownerId: Int): UserWithWatchlist?

    @Transaction
    @Query("SELECT * FROM User where userId = :ownerId")
    fun getAllItemsInFlow(ownerId: Int): Flow<UserWithWatchlist?>
}
