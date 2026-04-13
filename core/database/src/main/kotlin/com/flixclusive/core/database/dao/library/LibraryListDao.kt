package com.flixclusive.core.database.dao.library

import androidx.room.Dao
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.RoomRawQuery
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import com.flixclusive.core.database.entity.library.LibraryList
import com.flixclusive.core.database.entity.library.LibraryListItemWithMetadata
import com.flixclusive.core.database.entity.library.LibraryListType
import com.flixclusive.core.database.entity.library.LibraryListWithItems
import com.flixclusive.core.database.entity.library.SystemListDeletionException
import kotlinx.coroutines.flow.Flow

@Dao
interface LibraryListDao {
    @Query("SELECT * FROM library_lists WHERE ownerId = :userId")
    fun getAllAsFlow(userId: String): Flow<List<LibraryList>>

    @Query("SELECT * FROM library_lists WHERE ownerId = :userId")
    suspend fun getAll(userId: String): List<LibraryListWithItems>

    @Query("SELECT * FROM library_lists WHERE id = :id")
    fun getAsFlow(id: Int): Flow<LibraryList?>

    @Query("SELECT * FROM library_lists WHERE ownerId = :ownerId AND listType = :listType")
    fun getByTypeAsFlow(ownerId: String, listType: String): Flow<List<LibraryList>>

    @Transaction
    @Query("""
        SELECT DISTINCT list.*
        FROM library_lists list
        INNER JOIN library_list_items listItem ON list.id = listItem.listId
        WHERE listItem.filmId = :filmId AND list.ownerId = :ownerId
        ORDER BY list.createdAt DESC
    """)
    fun getListsContainingFilmAsFlow(filmId: String, ownerId: String): Flow<List<LibraryList>>

    @Query("SELECT * FROM library_lists WHERE listType = 'WATCHED' AND ownerId = :ownerId")
    suspend fun getWatchedList(ownerId: String): LibraryList

    @RawQuery(observedEntities = [LibraryList::class, LibraryListItemWithMetadata::class])
    fun getListsRaw(query: RoomRawQuery): Flow<List<LibraryListWithItems>>

    fun getLists(
        userId: String,
        columnSort: String,
        ascending: Boolean,
    ): Flow<List<LibraryListWithItems>> {
        val query = """
            SELECT * FROM library_lists
            WHERE ownerId = ?
            ORDER BY ${if (ascending) "$columnSort ASC" else "$columnSort DESC"}
        """.trimIndent()

        return getListsRaw(
            RoomRawQuery(
                sql = query,
                onBindStatement = { statement ->
                    statement.bindText(1, userId)
                }
            )
        )
    }

    @Upsert
    suspend fun insert(list: LibraryList): Long

    @Update
    suspend fun update(list: LibraryList)

    @Query("SELECT * FROM library_lists WHERE id = :id")
    suspend fun get(id: Int): LibraryList?

    /**
     * Guarded deletion — prevents deletion of system lists (WATCHLIST, CONTINUE_WATCHING).
     *
     * @throws SystemListDeletionException if the list is a system list.
     * */
    @Transaction
    suspend fun deleteSafe(id: Int) {
        val list = get(id) ?: return

        if (list.listType == LibraryListType.WATCHED) {
            throw SystemListDeletionException(list.listType)
        }

        deleteInternal(id)
    }

    @Query("DELETE FROM library_lists WHERE id = :listId")
    suspend fun deleteInternal(listId: Int)

    @Query("SELECT * FROM library_lists WHERE ownerId = :ownerId AND listType = :listType")
    suspend fun getByType(ownerId: String, listType: LibraryListType): List<LibraryList>

    @Query("DELETE FROM library_lists WHERE ownerId = :ownerId AND listType != 'WATCHED'")
    suspend fun deleteAllExceptWatched(ownerId: String)
}
