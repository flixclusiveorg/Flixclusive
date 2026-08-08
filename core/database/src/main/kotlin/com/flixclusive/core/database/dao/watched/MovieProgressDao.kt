package com.flixclusive.core.database.dao.watched

import androidx.room.Dao
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.RoomRawQuery
import androidx.room.Transaction
import androidx.room.Upsert
import com.flixclusive.core.database.entity.library.LibraryListItem
import com.flixclusive.core.database.entity.media.DBMedia
import com.flixclusive.core.database.entity.watched.MovieProgress
import com.flixclusive.core.database.entity.watched.MovieProgressWithMetadata
import com.flixclusive.core.database.entity.watched.WatchStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface MovieProgressDao {
    /*
     * Every read joins `media` rather than selecting the history table alone. MovieProgressWithMetadata
     * declares its media @Relation non-null, so a history row whose media is missing doesn't come back
     * empty — it throws, and takes the entire list with it. The foreign key on mediaId is supposed to
     * make that impossible, but it only guards writes made after it existed and is not enforced during
     * migrations, so rows predating it survive unnoticed. Joining keeps one bad row from being fatal.
     */

    @Transaction
    @Query(
        """
        SELECT h.* FROM movies_watch_history AS h
        INNER JOIN media AS m ON m.id = h.mediaId
        WHERE h.ownerId = :ownerId
        ORDER BY h.createdAt DESC
        """,
    )
    fun getAll(ownerId: String): List<MovieProgressWithMetadata>

    fun getAllAsFlow(
        ownerId: String,
        column: String,
        ascending: Boolean,
    ): Flow<List<MovieProgressWithMetadata>> {
        val query = """
            SELECT h.* FROM movies_watch_history AS h
            INNER JOIN media AS m ON m.id = h.mediaId
            WHERE h.ownerId = ?
            ORDER BY h.$column ${if (ascending) "ASC" else "DESC"}
        """.trimIndent()

        return getAllAsFlowRaw(
            RoomRawQuery(
                sql = query,
                onBindStatement = { statement ->
                    statement.bindText(1, ownerId)
                }
            )
        )
    }

    @RawQuery
    fun getAllAsFlowRaw(query: RoomRawQuery): Flow<List<MovieProgressWithMetadata>>

    @Transaction
    @Query(
        """
        SELECT h.* FROM movies_watch_history AS h
        INNER JOIN media AS m ON m.id = h.mediaId
        WHERE h.id = :id
        """,
    )
    suspend fun get(id: Long): MovieProgressWithMetadata?

    @Transaction
    @Query(
        """
        SELECT h.* FROM movies_watch_history AS h
        INNER JOIN media AS m ON m.id = h.mediaId
        WHERE h.mediaId = :id AND h.ownerId = :ownerId
        """,
    )
    suspend fun get(id: String, ownerId: String): MovieProgressWithMetadata?

    @Transaction
    @Query(
        """
        SELECT h.* FROM movies_watch_history AS h
        INNER JOIN media AS m ON m.id = h.mediaId
        WHERE h.id = :id
        """,
    )
    fun getAsFlow(id: Long): Flow<MovieProgressWithMetadata?>

    @Transaction
    @Query(
        """
        SELECT h.* FROM movies_watch_history AS h
        INNER JOIN media AS m ON m.id = h.mediaId
        WHERE h.mediaId = :id AND h.ownerId = :ownerId
        """,
    )
    fun getAsFlow(id: String, ownerId: String): Flow<MovieProgressWithMetadata?>

    @Transaction
    suspend fun insert(
        item: MovieProgress,
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
    suspend fun insertProgress(item: MovieProgress): Long

    @Upsert
    suspend fun insertMedia(media: DBMedia)

    @Upsert
    suspend fun insertListItem(item: LibraryListItem)

    @Query("DELETE FROM movies_watch_history WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM movies_watch_history WHERE ownerId = :ownerId")
    suspend fun deleteAll(ownerId: String)

    @Query(
        "UPDATE movies_watch_history " +
            "SET progress = :progress, status = :status, duration = :duration, createdAt = :watchedAt " +
            "WHERE id = :id AND mediaId = :mediaId",
    )
    suspend fun update(
        id: Long,
        mediaId: String,
        progress: Long,
        duration: Long,
        status: WatchStatus,
        watchedAt: Long?,
    )
}
