package com.flixclusive.core.database.dao.watched

import androidx.room.Dao
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.RoomRawQuery
import androidx.room.Transaction
import androidx.room.Upsert
import com.flixclusive.core.database.entity.library.LibraryListItem
import com.flixclusive.core.database.entity.media.DBMedia
import com.flixclusive.core.database.entity.watched.EpisodeProgress
import com.flixclusive.core.database.entity.watched.EpisodeProgressWithMetadata
import com.flixclusive.core.database.entity.watched.WatchStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface EpisodeProgressDao {
    @Transaction
    @Query(
        """
        SELECT * FROM series_watch_history
        WHERE ownerId = :ownerId
        ORDER BY createdAt DESC
        """,
    )
    fun getAll(ownerId: String): List<EpisodeProgressWithMetadata>

    fun getAllAsFlow(
        ownerId: String,
        column: String,
        ascending: Boolean,
    ): Flow<List<EpisodeProgressWithMetadata>> {
        val query = """
        SELECT * FROM series_watch_history s1
        WHERE ownerId = ?
        AND (seasonNumber, episodeNumber) = (
            SELECT seasonNumber, episodeNumber
            FROM series_watch_history s2
            WHERE s2.mediaId = s1.mediaId
            AND s2.ownerId = ?
            ORDER BY seasonNumber DESC, episodeNumber DESC
            LIMIT 1
        )
        ORDER BY $column ${if (ascending) "ASC" else "DESC"}
        """.trimIndent()

        return getAllAsFlowRaw(
            RoomRawQuery(
                sql = query,
                onBindStatement = { statement ->
                    statement.bindText(1, ownerId)
                    statement.bindText(2, ownerId)
                }
            )
        )
    }

    @RawQuery
    fun getAllAsFlowRaw(query: RoomRawQuery): Flow<List<EpisodeProgressWithMetadata>>

    @Transaction
    @Query("SELECT * FROM series_watch_history WHERE id = :id")
    suspend fun get(id: Long): EpisodeProgressWithMetadata?

    /**
     * Gets only the furthest episode watched for the given series.
     * */
    @Transaction
    @Query(
        """
        SELECT * FROM series_watch_history
        WHERE mediaId = :mediaId AND ownerId = :ownerId
        ORDER BY seasonNumber DESC, episodeNumber DESC
        LIMIT 1
        """,
    )
    suspend fun get(mediaId: String, ownerId: String): EpisodeProgressWithMetadata?

    @Transaction
    @Query("SELECT * FROM series_watch_history WHERE id = :id")
    fun getAsFlow(id: Long): Flow<EpisodeProgressWithMetadata?>

    /**
     * Gets only the furthest episode watched for the given series.
     * */
    @Transaction
    @Query(
        """
        SELECT * FROM series_watch_history
        WHERE mediaId = :itemId AND ownerId = :ownerId
        ORDER BY seasonNumber DESC, episodeNumber DESC
        LIMIT 1
        """,
    )
    fun getAsFlow(itemId: String, ownerId: String): Flow<EpisodeProgressWithMetadata?>

    @Query(
        """
        SELECT * FROM series_watch_history
        WHERE mediaId = :mediaId AND ownerId = :ownerId AND seasonNumber = :season
        ORDER BY episodeNumber ASC
        """,
    )
    suspend fun getSeasonProgress(mediaId: String, season: Int, ownerId: String): List<EpisodeProgress>

    @Query(
        """
        SELECT * FROM series_watch_history
        WHERE mediaId = :mediaId AND ownerId = :ownerId AND seasonNumber = :season AND episodeNumber = :episode
        LIMIT 1
        """,
    )
    suspend fun getEpisodeProgress(mediaId: String, season: Int, episode: Int, ownerId: String): EpisodeProgress?

    @Query(
        """
        SELECT * FROM series_watch_history
        WHERE mediaId = :mediaId AND ownerId = :ownerId AND seasonNumber = :season
        ORDER BY episodeNumber ASC
        """,
    )
    fun getSeasonProgressAsFlow(mediaId: String, season: Int, ownerId: String): Flow<List<EpisodeProgress>>

    @Transaction
    suspend fun insert(
        item: EpisodeProgress,
        listItem: LibraryListItem? = null,
        media: DBMedia? = null,
    ): Long {
        if (media != null) {
            insertMedia(media)
        }

        if (listItem != null) {
            insertListItem(listItem)
        }

        return insertProgress(item)
    }

    @Upsert
    suspend fun insertProgress(item: EpisodeProgress): Long

    @Upsert
    suspend fun insertMedia(media: DBMedia)

    @Upsert
    suspend fun insertListItem(item: LibraryListItem)

    @Query("DELETE FROM series_watch_history WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM series_watch_history WHERE ownerId = :ownerId")
    suspend fun deleteAll(ownerId: String)

    @Query(
        "UPDATE series_watch_history " +
            "SET progress = :progress, status = :status, duration = :duration, createdAt = :watchedAt " +
            "WHERE mediaId = :mediaId AND seasonNumber = :season AND episodeNumber = :episode AND id = :id",
    )
    suspend fun update(
        id: Long,
        mediaId: String,
        season: Int,
        episode: Int,
        progress: Long,
        duration: Long,
        status: WatchStatus,
        watchedAt: Long?,
    )
}
