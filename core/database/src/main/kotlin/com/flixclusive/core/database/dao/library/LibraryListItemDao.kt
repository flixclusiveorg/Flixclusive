package com.flixclusive.core.database.dao.library

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.RoomRawQuery
import androidx.room.Transaction
import androidx.room.Upsert
import com.flixclusive.core.database.entity.film.DBFilm
import com.flixclusive.core.database.entity.film.DBFilm.Companion.toDBFilm
import com.flixclusive.core.database.entity.film.DBFilmExternalId
import com.flixclusive.core.database.entity.film.DBFilmExternalId.Companion.toDBFilmExternalIds
import com.flixclusive.core.database.entity.film.DBFilmFts
import com.flixclusive.core.database.entity.film.DBFilmFts.Companion.toDBFilmFts
import com.flixclusive.core.database.entity.library.LibraryListItem
import com.flixclusive.core.database.entity.library.LibraryListItemWithMetadata
import com.flixclusive.model.film.Film
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface LibraryListItemDao {
    @Transaction
    @Query("SELECT * FROM library_list_item_with_metadata WHERE item_id = :id")
    suspend fun get(id: Long): LibraryListItemWithMetadata?

    @Transaction
    @Query("SELECT * FROM library_list_item_with_metadata WHERE item_id = :id")
    fun getAsFlow(id: Long): Flow<LibraryListItemWithMetadata?>

    @RawQuery
    fun getByListIdRaw(query: RoomRawQuery): Flow<List<LibraryListItemWithMetadata>>

    @Query("""
        SELECT * FROM library_list_item_with_metadata
        WHERE item_listId = :listId AND item_filmId = :filmId
        LIMIT 1
    """)
    fun getByListIdAndFilmId(listId: Int, filmId: String): LibraryListItemWithMetadata?

    fun getByListId(
        listId: Int,
        columnSort: String,
        ascending: Boolean,
    ): Flow<List<LibraryListItemWithMetadata>> {
        val query = """
            SELECT * FROM library_list_item_with_metadata
            WHERE item_listId = ?
            ORDER BY ${if (ascending) "$columnSort ASC" else "$columnSort DESC"}
        """.trimIndent()

        return getByListIdRaw(
            RoomRawQuery(
                sql = query,
                onBindStatement = { statement ->
                    statement.bindInt(1, listId)
                }
            )
        )
    }

    @RawQuery
    fun searchItemsRaw(query: RoomRawQuery): Flow<List<LibraryListItemWithMetadata>>

    fun searchItems(
        query: String,
        listId: Int,
        columnSort: String,
        ascending: Boolean,
    ): Flow<List<LibraryListItemWithMetadata>> {
        val ftsQuery = query
            .trim()
            .replace("\"", "")
            .let { if (it.isNotEmpty()) "\"$it*\"" else it }

        return searchItemsRaw(
            RoomRawQuery(
                sql = """
                    SELECT * FROM library_list_item_with_metadata
                    WHERE item_filmId IN (
                        SELECT filmId FROM films_fts WHERE films_fts MATCH ?
                    ) AND item_listId = ?
                    ORDER BY ${if (ascending) "$columnSort ASC" else "$columnSort DESC"}
                """.trimIndent(),
                onBindStatement = { statement ->
                    statement.bindText(1, ftsQuery)
                    statement.bindInt(2, listId)
                }
            )
        )
    }

    @Transaction
    suspend fun insert(
        item: LibraryListItem,
        film: Film? = null,
    ): Long {
        if (film != null) {
            upsertFilm(film.toDBFilm().copy(updatedAt = Date()))
            upsertFilmFts(film.toDBFilmFts())
            upsertIds(film.toDBFilmExternalIds())
        }

        return insertItem(item.copy(updatedAt = Date()))
    }

    @Query("DELETE FROM library_list_items WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM library_list_items WHERE listId = :listId AND filmId = :filmId")
    suspend fun deleteByListIdAndFilmId(listId: Int, filmId: String)


    @Upsert
    suspend fun upsertFilm(media: DBFilm)

    @Upsert
    suspend fun upsertFilmFts(mediaFts: DBFilmFts)

    @Upsert
    suspend fun upsertIds(list: List<DBFilmExternalId>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(list: LibraryListItem): Long
}
