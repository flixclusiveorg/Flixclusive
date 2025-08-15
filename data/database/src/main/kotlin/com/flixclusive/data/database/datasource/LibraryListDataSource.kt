package com.flixclusive.data.database.datasource

import com.flixclusive.core.database.entity.LibraryList
import com.flixclusive.core.database.entity.LibraryListAndItemCrossRef
import com.flixclusive.core.database.entity.LibraryListItem
import com.flixclusive.core.database.entity.LibraryListItemWithLists
import com.flixclusive.core.database.entity.LibraryListWithItems
import com.flixclusive.core.database.entity.UserWithLibraryListsAndItems
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
