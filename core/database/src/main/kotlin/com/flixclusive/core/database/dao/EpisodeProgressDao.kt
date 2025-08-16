package com.flixclusive.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.flixclusive.core.database.entity.film.DBFilm
import com.flixclusive.core.database.entity.watched.EpisodeProgress
import com.flixclusive.core.database.entity.watched.EpisodeProgressWithMetadata
import com.flixclusive.core.database.entity.watched.WatchStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface EpisodeProgressDao {
    @Transaction
    @Query(
        """
        SELECT wh.*
        FROM series_watch_history AS wh
        JOIN User AS u
        ON wh.ownerId = u.userId
        WHERE u.userId = :ownerId
        ORDER BY wh.watchedAt DESC;
        """,
    )
    fun getAllAsFlow(ownerId: Int): Flow<List<EpisodeProgressWithMetadata>>

    @Transaction
    @Query(
        """
        SELECT wh.*
        FROM series_watch_history AS wh
        JOIN User AS u
        ON wh.ownerId = u.userId
        WHERE userId = :ownerId
        ORDER BY RANDOM() LIMIT :count
        """,
    )
    fun getRandoms(
        ownerId: Int,
        count: Int,
    ): Flow<List<EpisodeProgressWithMetadata>>

    @Transaction
    @Query(
        """
        SELECT * FROM series_watch_history
        WHERE filmId = :seriesId AND ownerId = :ownerId
        ORDER BY seasonNumber, episodeNumber
        """,
    )
    fun getAllForSeries(
        seriesId: String,
        ownerId: Int,
    ): Flow<List<EpisodeProgressWithMetadata>>

    @Transaction
    @Query("SELECT * FROM series_watch_history WHERE id = :itemId")
    suspend fun get(itemId: Long): EpisodeProgressWithMetadata?

    @Transaction
    @Query("SELECT * FROM series_watch_history WHERE id = :itemId")
    fun getAsFlow(itemId: Long): Flow<EpisodeProgressWithMetadata?>

    @Transaction
    suspend fun insert(
        item: EpisodeProgress,
        film: DBFilm? = null,
    ): Long {
        if (film != null) {
            insertFilm(film)
        }

        return insertProgress(item)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgress(item: EpisodeProgress): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFilm(film: DBFilm)

    @Query("DELETE FROM series_watch_history WHERE id = :itemId")
    suspend fun delete(itemId: Long)

    @Query("DELETE FROM series_watch_history WHERE ownerId = :ownerId")
    suspend fun deleteAll(ownerId: Int)

    @Query(
        "UPDATE series_watch_history " +
            "SET progress = :progress, status = :status, duration = :duration, watchedAt = :watchedAt " +
            "WHERE filmId = :filmId AND seasonNumber = :season AND episodeNumber = :episode AND id = :itemId",
    )
    suspend fun update(
        itemId: Long,
        filmId: String,
        season: Int,
        episode: Int,
        progress: Long,
        duration: Long,
        status: WatchStatus,
        watchedAt: Long?,
    )
}
