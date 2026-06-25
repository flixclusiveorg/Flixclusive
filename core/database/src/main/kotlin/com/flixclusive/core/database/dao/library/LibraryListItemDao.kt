package com.flixclusive.core.database.dao.library

import androidx.room.Dao
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.RoomRawQuery
import androidx.room.Transaction
import androidx.room.Upsert
import com.flixclusive.core.database.entity.library.LibraryListItem
import com.flixclusive.core.database.entity.library.LibraryListItemWithMetadata
import com.flixclusive.core.database.entity.media.DBMedia
import com.flixclusive.core.database.entity.media.DBMedia.Companion.toDBMedia
import com.flixclusive.core.database.entity.media.DBMediaExternalId
import com.flixclusive.core.database.entity.media.DBMediaExternalId.Companion.toDBMediaExternalIds
import com.flixclusive.core.database.entity.media.DBMediaFts
import com.flixclusive.core.database.entity.media.DBMediaFts.Companion.toDBMediaFts
import com.flixclusive.model.media.MediaMetadata
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface LibraryListItemDao {
    @Transaction
    @Query("SELECT * FROM library_list_item_with_metadata WHERE item_id = :id")
    suspend fun get(id: String): LibraryListItemWithMetadata?

    @Transaction
    @Query("SELECT * FROM library_list_item_with_metadata WHERE item_id = :id")
    fun getAsFlow(id: String): Flow<LibraryListItemWithMetadata?>

    @Transaction
    @RawQuery(observedEntities = [LibraryListItemWithMetadata::class])
    suspend fun getByListIdRaw(query: RoomRawQuery): List<LibraryListItemWithMetadata>

    @Transaction
    @Query(
        """
        SELECT * FROM library_list_item_with_metadata
        WHERE item_listId = :listId AND item_mediaId = :mediaId
        LIMIT 1
    """
    )
    suspend fun getByListIdAndMediaId(listId: String, mediaId: String): LibraryListItemWithMetadata?

    @Transaction
    suspend fun paginateByListId(
        listId: String,
        columnSort: String,
        ascending: Boolean,
        pageSize: Int,
        page: Int,
    ): List<LibraryListItemWithMetadata> {
        val query = """
            SELECT * FROM library_list_item_with_metadata
            WHERE item_listId = ?
            ORDER BY ${if (ascending) "$columnSort ASC" else "$columnSort DESC"}
            LIMIT $pageSize OFFSET ${pageSize * (page - 1)}
        """.trimIndent()

        return getByListIdRaw(
            RoomRawQuery(
                sql = query,
                onBindStatement = { statement ->
                    statement.bindText(1, listId)
                }
            )
        )
    }

    @RawQuery(observedEntities = [LibraryListItemWithMetadata::class])
    fun searchItemsRaw(query: RoomRawQuery): Flow<List<LibraryListItemWithMetadata>>

    fun searchItems(
        query: String,
        listId: String,
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
                    WHERE item_mediaId IN (
                        SELECT mediaId FROM medias_fts WHERE medias_fts MATCH ?
                    ) AND item_listId = ?
                    ORDER BY ${if (ascending) "$columnSort ASC" else "$columnSort DESC"}
                """.trimIndent(),
                onBindStatement = { statement ->
                    statement.bindText(1, ftsQuery)
                    statement.bindText(2, listId)
                }
            )
        )
    }

    @Transaction
    suspend fun insert(
        item: LibraryListItem,
        media: MediaMetadata? = null,
    ): String {
        if (media != null) {
            upsertMedia(media.toDBMedia().copy(updatedAt = Date()))
            upsertMediaFts(media.toDBMediaFts())
            upsertIds(media.toDBMediaExternalIds())
        }

        val updatedItem = item.copy(updatedAt = Date())
        insertItem(updatedItem)
        return updatedItem.id
    }

    @Query("DELETE FROM library_list_items WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM library_list_items WHERE listId = :listId AND mediaId = :mediaId")
    suspend fun deleteByListIdAndMediaId(listId: String, mediaId: String)

    @Upsert
    suspend fun upsertMedia(media: DBMedia)

    @Upsert
    suspend fun upsertMediaFts(mediaFts: DBMediaFts)

    @Upsert
    suspend fun upsertIds(list: List<DBMediaExternalId>)

    @Upsert
    suspend fun insertItem(item: LibraryListItem)
}
