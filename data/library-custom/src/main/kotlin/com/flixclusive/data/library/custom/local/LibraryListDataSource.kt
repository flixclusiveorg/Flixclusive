package com.flixclusive.data.library.custom.local

import com.flixclusive.model.database.LibraryList
import com.flixclusive.model.database.LibraryListAndItemCrossRef
import com.flixclusive.model.database.LibraryListItem
import com.flixclusive.model.database.LibraryListItemWithLists
import com.flixclusive.model.database.LibraryListWithItems
import com.flixclusive.model.database.UserWithLibraryListsAndItems
import kotlinx.coroutines.flow.Flow

interface LibraryListDataSource {
    fun getLists(userId: Int): Flow<List<LibraryList>>

    fun getList(listId: Int): Flow<LibraryList?>

    suspend fun insertList(list: LibraryList)

    suspend fun updateList(list: LibraryList)

    suspend fun deleteListById(listId: Int)

    fun getItem(itemId: String): Flow<LibraryListItem?>

    suspend fun addItemToList(
        listId: Int,
        item: LibraryListItem,
    )

    suspend fun updateItem(item: LibraryListItem)

    fun getItemWithLists(itemId: String): Flow<LibraryListItemWithLists?>

    suspend fun deleteItemFromList(
        listId: Int,
        itemId: String,
    )

    fun getListWithItems(listId: Int): Flow<LibraryListWithItems?>

    fun getUserWithListsAndItems(userId: Int): Flow<UserWithLibraryListsAndItems>

    fun getCrossRef(
        listId: Int,
        itemId: String,
    ): Flow<LibraryListAndItemCrossRef?>
}
