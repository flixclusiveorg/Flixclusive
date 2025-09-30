package com.flixclusive.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.flixclusive.core.database.entity.film.DBFilm
import com.flixclusive.core.database.entity.watched.MovieProgress
import com.flixclusive.core.database.entity.watched.MovieProgressWithMetadata
import com.flixclusive.core.database.entity.watched.WatchStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface MovieProgressDao {
    @Transaction
    @Query("SELECT * FROM movies_watch_history WHERE ownerId = :ownerId ORDER BY watchedAt DESC")
    fun getAllAsFlow(ownerId: Int): Flow<List<MovieProgressWithMetadata>>

    @Transaction
    @Query("SELECT * FROM movies_watch_history WHERE ownerId = :ownerId ORDER BY RANDOM() LIMIT :count")
    fun getRandoms(
        ownerId: Int,
        count: Int,
    ): Flow<List<MovieProgressWithMetadata>>

    @Transaction
    @Query("SELECT * FROM movies_watch_history WHERE id = :itemId")
    suspend fun get(itemId: Long): MovieProgressWithMetadata?

    @Transaction
    @Query("SELECT * FROM movies_watch_history WHERE filmId = :itemId AND ownerId = :ownerId")
    suspend fun get(itemId: String, ownerId: Int): MovieProgressWithMetadata?

    @Transaction
    @Query("SELECT * FROM movies_watch_history WHERE id = :itemId")
    fun getAsFlow(itemId: Long): Flow<MovieProgressWithMetadata?>

    @Transaction
    @Query("SELECT * FROM movies_watch_history WHERE filmId = :itemId AND ownerId = :ownerId")
    fun getAsFlow(itemId: String, ownerId: Int): Flow<MovieProgressWithMetadata?>

    @Transaction
    suspend fun insert(
        item: MovieProgress,
        film: DBFilm? = null,
    ): Long {
        if (film != null) {
            insertFilm(film)
        }

        return insertProgress(item)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgress(item: MovieProgress): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFilm(film: DBFilm)

    @Query("DELETE FROM movies_watch_history WHERE id = :itemId")
    suspend fun delete(itemId: Long)

    @Query("DELETE FROM movies_watch_history WHERE ownerId = :ownerId")
    suspend fun deleteAll(ownerId: Int)

    @Query(
        "UPDATE movies_watch_history " +
            "SET progress = :progress, status = :status, duration = :duration, watchedAt = :watchedAt " +
            "WHERE id = :itemId AND filmId = :filmId",
    )
    suspend fun update(
        itemId: Long,
        filmId: String,
        progress: Long,
        duration: Long,
        status: WatchStatus,
        watchedAt: Long?,
    )
}
