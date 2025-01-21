package com.flixclusive.data.library.custom

import com.flixclusive.model.database.LibraryList
import com.flixclusive.model.database.LibraryListAndItemCrossRef
import com.flixclusive.model.database.LibraryListItem
import com.flixclusive.model.database.LibraryListItemWithLists
import com.flixclusive.model.database.LibraryListWithItems
import com.flixclusive.model.database.UserWithLibraryListsAndItems
import kotlinx.coroutines.flow.Flow

interface LibraryListRepository {
    // Library List operations
    fun getLists(userId: Int): Flow<List<LibraryList>>
    fun getList(listId: Int): Flow<LibraryList?>
    fun getListWithItems(listId: Int): Flow<LibraryListWithItems?>

    suspend fun insertList(list: LibraryList)

    suspend fun updateList(list: LibraryList)

    suspend fun deleteList(list: LibraryList)

    suspend fun deleteListById(listId: Int)

    // List Item operations
    fun getItem(itemId: String): Flow<LibraryListItem?>
    fun getItemWithLists(itemId: String): Flow<LibraryListItemWithLists?>

    suspend fun insertItem(item: LibraryListItem)

    suspend fun updateItem(item: LibraryListItem)

    suspend fun deleteItem(item: LibraryListItem)

    // Cross Reference operations
    suspend fun insertCrossRef(crossRef: LibraryListAndItemCrossRef)

    suspend fun deleteCrossRef(crossRef: LibraryListAndItemCrossRef)

    suspend fun deleteCrossRefById(
        listId: Int,
        itemId: String,
    )

    fun getUserWithListsAndItems(userId: Int): Flow<UserWithLibraryListsAndItems?>
    fun getItemAddedDetails(
        listId: Int,
        itemId: String,
    ): Flow<LibraryListAndItemCrossRef?>
}
