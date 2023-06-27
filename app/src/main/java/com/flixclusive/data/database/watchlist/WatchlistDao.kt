package com.flixclusive.data.database.watchlist

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.flixclusive.domain.model.entities.WatchlistItem
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchlistDao {
    @Insert
    suspend fun insert(item: WatchlistItem)

    @Delete
    suspend fun delete(item: WatchlistItem)

    @Query("DELETE FROM watchlist WHERE id = :itemId")
    suspend fun deleteById(itemId: Int)

    @Query("SELECT * FROM watchlist WHERE id = :filmId")
    suspend fun getWatchlistItemById(filmId: Int): WatchlistItem?

    @Query("SELECT * FROM watchlist")
    suspend fun getAllItems(): List<WatchlistItem>

    @Query("SELECT * FROM watchlist")
    fun getAllItemsInFlow(): Flow<List<WatchlistItem>>
}
