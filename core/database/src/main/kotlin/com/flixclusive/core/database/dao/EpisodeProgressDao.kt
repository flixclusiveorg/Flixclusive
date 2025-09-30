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
    /**
     * Gets the latest watched episode for each series.
     *
     * This only returns the furthest episode watched in each series, not all episodes.
     * */
    @Transaction
    @Query(
        """
        SELECT * FROM series_watch_history s1
        WHERE ownerId = :ownerId
        AND (seasonNumber, episodeNumber) = (
            SELECT seasonNumber, episodeNumber
            FROM series_watch_history s2
            WHERE s2.filmId = s1.filmId
            AND s2.ownerId = :ownerId
            ORDER BY seasonNumber DESC, episodeNumber DESC
            LIMIT 1
        )
        ORDER BY watchedAt DESC
        """,
    )
    fun getAllAsFlow(ownerId: Int): Flow<List<EpisodeProgressWithMetadata>>

    @Transaction
    @Query(
        """
        SELECT * FROM series_watch_history
        WHERE ownerId = :ownerId
        ORDER BY RANDOM() LIMIT :count
        """,
    )
    fun getRandoms(
        ownerId: Int,
        count: Int,
    ): Flow<List<EpisodeProgressWithMetadata>>

    @Transaction
    @Query("SELECT * FROM series_watch_history WHERE id = :itemId")
    suspend fun get(itemId: Long): EpisodeProgressWithMetadata?

    /**
     * Gets only the furthest episode watched for the given series.
     * */
    @Transaction
    @Query(
        """
        SELECT * FROM series_watch_history
        WHERE filmId = :itemId AND ownerId = :ownerId
        ORDER BY seasonNumber DESC, episodeNumber DESC
        LIMIT 1
        """,
    )
    suspend fun get(itemId: String, ownerId: Int): EpisodeProgressWithMetadata?

    @Transaction
    @Query("SELECT * FROM series_watch_history WHERE id = :itemId")
    fun getAsFlow(itemId: Long): Flow<EpisodeProgressWithMetadata?>

    /**
     * Gets only the furthest episode watched for the given series.
     * */
    @Transaction
    @Query(
        """
        SELECT * FROM series_watch_history
        WHERE filmId = :itemId AND ownerId = :ownerId
        ORDER BY seasonNumber DESC, episodeNumber DESC
        LIMIT 1
        """,
    )
    fun getAsFlow(itemId: String, ownerId: Int): Flow<EpisodeProgressWithMetadata?>

    @Query(
        """
        SELECT * FROM series_watch_history
        WHERE filmId = :itemId AND ownerId = :ownerId AND seasonNumber = :season
        ORDER BY episodeNumber ASC
        """,
    )
    suspend fun getSeasonProgress(itemId: String, season: Int, ownerId: Int): List<EpisodeProgress>

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
