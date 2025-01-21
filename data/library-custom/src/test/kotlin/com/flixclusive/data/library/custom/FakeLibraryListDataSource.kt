package com.flixclusive.data.library.custom

import com.flixclusive.data.library.custom.local.LibraryListDataSource
import com.flixclusive.model.database.LibraryList
import com.flixclusive.model.database.LibraryListAndItemCrossRef
import com.flixclusive.model.database.LibraryListItem
import com.flixclusive.model.database.LibraryListItemWithLists
import com.flixclusive.model.database.LibraryListWithItems
import com.flixclusive.model.database.User
import com.flixclusive.model.database.UserWithLibraryListsAndItems
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

internal class FakeLibraryListDataSource : LibraryListDataSource {
    private val libraryLists = MutableStateFlow<List<LibraryList>>(emptyList())
    private val libraryListItems = MutableStateFlow<List<LibraryListItem>>(emptyList())
    private val crossRefs = MutableStateFlow<List<LibraryListAndItemCrossRef>>(emptyList())

    // Library List operations
    override fun getLists(userId: Int): Flow<List<LibraryList>> =
        libraryLists.map { lists -> lists.filter { it.ownerId == userId } }

    override fun getList(listId: Int): Flow<LibraryList?> =
        libraryLists.map { lists ->
            lists.find { it.id == listId }
        }

    override fun getListWithItems(listId: Int): Flow<LibraryListWithItems?> =
        combine(
            libraryLists,
            crossRefs,
            libraryListItems,
        ) { lists, refs, items ->
            val list = lists.find { it.id == listId }
            val itemIds = refs.filter { it.listId == listId }.map { it.itemId }
            val listItems = items.filter { itemIds.contains(it.id) }
            list?.let { LibraryListWithItems(it, listItems) }
        }

    override suspend fun insertList(list: LibraryList) {
        libraryLists.update { current ->
            current.filterNot { it.id == list.id } + list
        }
    }

    override suspend fun updateList(list: LibraryList) = insertList(list)

    override suspend fun deleteList(list: LibraryList) {
        libraryLists.update { current -> current.filterNot { it.id == list.id } }
    }

    override suspend fun deleteListById(listId: Int) {
        libraryLists.update { current -> current.filterNot { it.id == listId } }
    }

    // List Item operations
    override fun getItem(itemId: String): Flow<LibraryListItem?> =
        libraryListItems.map { items -> items.find { it.id == itemId } }

    override fun getItemWithLists(itemId: String): Flow<LibraryListItemWithLists?> =
        combine(
            libraryLists,
            libraryListItems,
            crossRefs,
        ) { lists, items, refs ->
            val item = items.find { it.id == itemId }
            val listIds = refs.filter { it.itemId == itemId }.map { it.listId }
            val itemLists = lists.filter { listIds.contains(it.id) }
            item?.let { LibraryListItemWithLists(it, itemLists) }
        }

    override suspend fun insertItem(item: LibraryListItem) {
        libraryListItems.update { current ->
            current.filterNot { it.id == item.id } + item
        }
    }

    override suspend fun updateItem(item: LibraryListItem) = insertItem(item)

    override suspend fun deleteItem(item: LibraryListItem) {
        libraryListItems.update { current -> current.filterNot { it.id == item.id } }
    }

    // Cross Reference operations
    override suspend fun insertCrossRef(crossRef: LibraryListAndItemCrossRef) {
        crossRefs.update { current ->
            current.filterNot {
                it.listId == crossRef.listId && it.itemId == crossRef.itemId
            } + crossRef
        }
    }

    override suspend fun deleteCrossRef(crossRef: LibraryListAndItemCrossRef) {
        crossRefs.update { current ->
            current.filterNot {
                it.listId == crossRef.listId && it.itemId == crossRef.itemId
            }
        }
    }

    override suspend fun deleteCrossRefById(
        listId: Int,
        itemId: String,
    ) {
        crossRefs.update { current ->
            current.filterNot {
                it.listId == listId && it.itemId == itemId
            }
        }
    }

    override fun getUserWithListsAndItems(userId: Int): Flow<UserWithLibraryListsAndItems?> =
        combine(
            libraryLists,
            crossRefs,
            libraryListItems,
        ) { lists, refs, items ->
            val user = User(userId, name = "Test name", image = 0)
            val userLists = lists.filter { it.ownerId == userId }
            val listsWithItems =
                userLists.map { list ->
                    val listItems =
                        items.filter { item ->
                            refs.any { ref ->
                                ref.listId == list.id && ref.itemId == item.id
                            }
                        }
                    LibraryListWithItems(list, listItems)
                }
            UserWithLibraryListsAndItems(user, listsWithItems)
        }

    override fun getItemAddedDetails(
        listId: Int,
        itemId: String,
    ): Flow<LibraryListAndItemCrossRef?> =
        crossRefs.map { refs ->
            refs.find { it.listId == listId && it.itemId == itemId }
        }
}
