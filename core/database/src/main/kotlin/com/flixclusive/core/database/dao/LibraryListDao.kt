package com.flixclusive.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.flixclusive.model.database.LibraryList
import com.flixclusive.model.database.LibraryListItem
import kotlinx.coroutines.flow.Flow

@Dao
interface LibraryListDao {
    @Insert
    suspend fun createList(list: LibraryList)

    @Delete
    suspend fun removeList(list: LibraryList)

    @Insert
    suspend fun addItemToList(item: LibraryListItem)

    @Delete
    suspend fun removeItemFromList(item: LibraryListItem)

    @Query("SELECT * FROM library_lists WHERE ownerId = :ownerId")
    suspend fun getLibraryLists(ownerId: Int): List<LibraryList>

    @Query("SELECT * FROM library_lists WHERE listId = :listId")
    suspend fun getLibraryList(listId: String): LibraryList?

    @Transaction
    @Query(
        """
        SELECT * FROM library_list_entries
        WHERE listId = :listId
        ORDER BY addedAt DESC
    """,
    )
    suspend fun getListItems(listId: String): List<LibraryListItem>


    @Transaction
    @Query("SELECT * FROM library_list_entries WHERE entryId = :id")
    suspend fun getListItem(id: Long): LibraryListItem?

    @Query("SELECT * FROM library_lists WHERE ownerId = :ownerId")
    fun getLibraryListsAsFlow(ownerId: Int): Flow<List<LibraryList>>

    @Query("SELECT * FROM library_lists WHERE listId = :listId")
    fun getLibraryListAsFlow(listId: String): Flow<LibraryList?>

    @Transaction
    @Query("SELECT * FROM library_list_entries WHERE entryId = :id")
    fun getListItemAsFlow(id: Long): Flow<LibraryListItem?>

    @Transaction
    @Query(
        """
        SELECT * FROM library_list_entries
        WHERE listId = :listId
        ORDER BY addedAt DESC
    """,
    )
    fun getListItemsAsFlow(listId: String): Flow<List<LibraryListItem>>
}
