package com.flixclusive.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.flixclusive.core.database.entity.film.DBFilm
import com.flixclusive.core.database.entity.watchlist.Watchlist
import com.flixclusive.core.database.entity.watchlist.WatchlistWithMetadata
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchlistDao {
    @Transaction
    suspend fun insert(
        item: Watchlist,
        film: DBFilm? = null,
    ): Long {
        if (film != null) {
            insertFilm(film)
        }

        return insertWatchlist(item)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWatchlist(watchlist: Watchlist): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFilm(film: DBFilm)

    @Query("SELECT EXISTS(SELECT 1 FROM watchlist WHERE filmId = :filmId AND ownerId = :ownerId)")
    suspend fun isInWatchlist(
        filmId: String,
        ownerId: Int,
    ): Boolean

    @Query("DELETE FROM watchlist WHERE ownerId = :ownerId")
    suspend fun deleteAll(ownerId: Int)

    @Query("DELETE FROM watchlist WHERE id = :id")
    suspend fun delete(id: Long)

    @Transaction
    @Query("SELECT * FROM watchlist WHERE id = :id")
    suspend fun get(id: Long): WatchlistWithMetadata?

    @Transaction
    @Query("SELECT * FROM watchlist WHERE ownerId = :ownerId ORDER BY addedAt DESC")
    suspend fun getAll(ownerId: Int): List<WatchlistWithMetadata>

    @Transaction
    @Query("SELECT * FROM watchlist WHERE ownerId = :ownerId ORDER BY addedAt DESC")
    fun getAllAsFlow(ownerId: Int): Flow<List<WatchlistWithMetadata>>
}
