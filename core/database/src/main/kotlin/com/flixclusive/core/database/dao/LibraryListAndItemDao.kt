package com.flixclusive.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.flixclusive.model.database.LibraryListAndItemCrossRef
import com.flixclusive.model.database.LibraryListItemWithLists
import com.flixclusive.model.database.LibraryListWithItems
import com.flixclusive.model.database.UserWithLibraryListsAndItems
import kotlinx.coroutines.flow.Flow

@Dao
interface LibraryListAndItemDao {
    @Insert
    suspend fun addItemToList(crossRef: LibraryListAndItemCrossRef)

    @Query("DELETE FROM library_list_and_item_cross_ref WHERE listId = :listId AND itemId = :itemId")
    suspend fun deleteItemFromList(listId: Int, itemId: String)

    @Transaction
    @Query("SELECT * FROM library_list_items WHERE itemId = :itemId")
    fun getItemWithLists(itemId: String): Flow<LibraryListItemWithLists?>

    @Transaction
    @Query("SELECT * FROM library_lists WHERE listId = :listId")
    fun getListWithItems(listId: Int): Flow<LibraryListWithItems?>

    @Transaction
    @Query("SELECT * FROM User WHERE userId = :userId")
    fun getUserWithListsAndItems(userId: Int): Flow<UserWithLibraryListsAndItems>

    @Transaction
    @Query("SELECT * FROM library_list_and_item_cross_ref WHERE listId = :listId AND itemId = :itemId")
    fun getCrossRef(listId: Int, itemId: String): Flow<LibraryListAndItemCrossRef?>
}
