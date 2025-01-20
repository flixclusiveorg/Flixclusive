package com.flixclusive.data.library.custom.local

import com.flixclusive.model.database.LibraryList
import com.flixclusive.model.database.LibraryListItem
import kotlinx.coroutines.flow.Flow

interface LibraryListDataSource {
    suspend fun createList(list: LibraryList)

    suspend fun removeList(list: LibraryList)

    suspend fun addItemToList(item: LibraryListItem)

    suspend fun removeItemFromList(item: LibraryListItem)

    suspend fun getLibraryLists(ownerId: Int): List<LibraryList>

    suspend fun getLibraryList(listId: String): LibraryList?

    suspend fun getListItem(id: Long): LibraryListItem?

    suspend fun getListItems(listId: String): List<LibraryListItem>

    fun getLibraryListsAsFlow(ownerId: Int): Flow<List<LibraryList>>

    fun getLibraryListAsFlow(listId: String): Flow<LibraryList?>

    fun getListItemAsFlow(id: Long): Flow<LibraryListItem?>

    fun getListItemsAsFlow(listId: String): Flow<List<LibraryListItem>>
}
