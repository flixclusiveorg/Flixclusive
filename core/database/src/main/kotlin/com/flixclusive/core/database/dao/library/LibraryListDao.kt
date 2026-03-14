package com.flixclusive.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.flixclusive.core.database.entity.library.LibraryList
import com.flixclusive.core.database.entity.library.LibraryListType
import com.flixclusive.core.database.entity.library.LibraryListWithItems
import com.flixclusive.core.database.entity.library.SystemListDeletionException
import com.flixclusive.core.database.entity.library.UserWithLibraryListsAndItems
import kotlinx.coroutines.flow.Flow

@Dao
interface LibraryListDao {
    @Query("SELECT * FROM library_lists WHERE ownerId = :userId")
    fun getAllAsFlow(userId: Int): Flow<List<LibraryList>>

    @Query("SELECT * FROM library_lists WHERE id = :id")
    fun getAsFlow(id: Int): Flow<LibraryList?>

    @Query("SELECT * FROM library_lists WHERE ownerId = :ownerId AND listType = :listType")
    fun getByTypeAsFlow(ownerId: Int, listType: String): Flow<List<LibraryList>>

    @Transaction
    @Query("""
        SELECT DISTINCT list.*
        FROM library_lists list
        INNER JOIN library_list_items listItem ON list.id = listItem.listId
        WHERE listItem.filmId = :filmId AND list.ownerId = :ownerId
        ORDER BY list.createdAt DESC
    """)
    fun getListsContainingFilmAsFlow(filmId: String, ownerId: Int): Flow<List<LibraryList>>

    @Transaction
    @Query("SELECT * FROM library_lists WHERE id = :id")
    fun getListWithItemsAsFlow(id: Int): Flow<LibraryListWithItems?>

    @Transaction
    @Query("SELECT * FROM User WHERE userId = :userId")
    fun getUserWithListsAndItemsAsFlow(userId: Int): Flow<UserWithLibraryListsAndItems>

    @Insert
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

    /**
     * Seeds system lists (WATCHLIST and CONTINUE_WATCHING) for the given user.
     * Skips insertion if they already exist.
     * */
    @Transaction
    suspend fun seedWatchedList(userId: Int) {
        val existingCW = getByType(userId, LibraryListType.WATCHED)
        if (existingCW.isNotEmpty()) return

        insert(
            LibraryList(
                ownerId = userId,
                name = "Continue Watching",
                listType = LibraryListType.WATCHED,
            )
        )
    }

    @Query("SELECT * FROM library_lists WHERE ownerId = :ownerId AND listType = :listType")
    suspend fun getByType(ownerId: Int, listType: LibraryListType): List<LibraryList>
}
