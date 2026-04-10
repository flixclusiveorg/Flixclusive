package com.flixclusive.core.database.dao.watched

import androidx.room.Dao
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.RoomRawQuery
import androidx.room.Transaction
import androidx.room.Upsert
import com.flixclusive.core.database.entity.film.DBFilm
import com.flixclusive.core.database.entity.library.LibraryListItem
import com.flixclusive.core.database.entity.watched.MovieProgress
import com.flixclusive.core.database.entity.watched.MovieProgressWithMetadata
import com.flixclusive.core.database.entity.watched.WatchStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface MovieProgressDao {
    @Query(
        """
        SELECT * FROM movies_watch_history
        WHERE ownerId = :ownerId
        ORDER BY createdAt DESC
        """,
    )
    fun getAll(ownerId: Int): List<MovieProgressWithMetadata>

    fun getAllAsFlow(
        ownerId: Int,
        column: String,
        ascending: Boolean,
    ): Flow<List<MovieProgressWithMetadata>> {
        val query = """
            SELECT * FROM movies_watch_history WHERE ownerId = ?
            ORDER BY $column ${if (ascending) "ASC" else "DESC"}
        """.trimIndent()

        return getAllAsFlowRaw(
            RoomRawQuery(
                sql = query,
                onBindStatement = { statement ->
                    statement.bindInt(1, ownerId)
                }
            )
        )
    }

    @RawQuery
    fun getAllAsFlowRaw(query: RoomRawQuery): Flow<List<MovieProgressWithMetadata>>

    @Transaction
    @Query("SELECT * FROM movies_watch_history WHERE ownerId = :ownerId ORDER BY RANDOM() LIMIT :count")
    fun getRandoms(
        ownerId: Int,
        count: Int,
    ): Flow<List<MovieProgressWithMetadata>>

    @Transaction
    @Query("SELECT * FROM movies_watch_history WHERE id = :id")
    suspend fun get(id: Long): MovieProgressWithMetadata?

    @Transaction
    @Query("SELECT * FROM movies_watch_history WHERE filmId = :id AND ownerId = :ownerId")
    suspend fun get(id: String, ownerId: Int): MovieProgressWithMetadata?

    @Transaction
    @Query("SELECT * FROM movies_watch_history WHERE id = :id")
    fun getAsFlow(id: Long): Flow<MovieProgressWithMetadata?>

    @Transaction
    @Query("SELECT * FROM movies_watch_history WHERE filmId = :id AND ownerId = :ownerId")
    fun getAsFlow(id: String, ownerId: Int): Flow<MovieProgressWithMetadata?>

    @Transaction
    suspend fun insert(
        item: MovieProgress,
        listItem: LibraryListItem? = null,
        film: DBFilm? = null,
    ): Long {
        if (film != null) {
            insertFilm(film)
        }

        if (listItem != null) {
            insertListItem(listItem)
        }

        return insertProgress(item)
    }

    @Upsert
    suspend fun insertProgress(item: MovieProgress): Long

    @Upsert
    suspend fun insertFilm(film: DBFilm)

    @Upsert
    suspend fun insertListItem(item: LibraryListItem)

    @Query("DELETE FROM movies_watch_history WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM movies_watch_history WHERE ownerId = :ownerId")
    suspend fun deleteAll(ownerId: Int)

    @Query(
        "UPDATE movies_watch_history " +
            "SET progress = :progress, status = :status, duration = :duration, createdAt = :watchedAt " +
            "WHERE id = :id AND filmId = :filmId",
    )
    suspend fun update(
        id: Long,
        filmId: String,
        progress: Long,
        duration: Long,
        status: WatchStatus,
        watchedAt: Long?,
    )
}
