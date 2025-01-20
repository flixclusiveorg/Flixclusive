package com.flixclusive.data.library.custom

import com.flixclusive.data.library.custom.local.LibraryListDataSource
import com.flixclusive.model.database.LibraryItemId
import com.flixclusive.model.database.LibraryList
import com.flixclusive.model.database.LibraryListItem
import com.flixclusive.model.film.DEFAULT_FILM_SOURCE_NAME
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
internal class FakeLibraryListDataSource : LibraryListDataSource {
    private val lists = mutableSetOf<LibraryList>()
    private val listItems = mutableSetOf<LibraryListItem>()

    init {
        lists.addAll(
            List(50) {
                LibraryList(
                    id = Uuid.random().toString(),
                    ownerId = 1,
                    name = "Catalog #$it"
                )
            }
        )

        lists.forEach { list ->
            listItems.addAll(
                List(5) {
                    val size = listItems.size
                    LibraryListItem(
                        entryId = size.toLong(),
                        listId = list.id,
                        libraryItemId = LibraryItemId(
                            providerId = DEFAULT_FILM_SOURCE_NAME,
                            itemId = "item-$size",
                        )
                    )
                }
            )
        }
    }

    override suspend fun createList(list: LibraryList) {
        lists.add(list)
    }

    override suspend fun removeList(list: LibraryList) {
        val isListIdValid = lists.any { list.id == it.id }
        if (isListIdValid) {
            lists.remove(list)
        }
    }

    override suspend fun addItemToList(item: LibraryListItem) {
        val isListIdValid = lists.any { item.listId == it.id }
        if (isListIdValid) {
            listItems.add(item)
        }
    }

    override suspend fun removeItemFromList(item: LibraryListItem) {
        val isListIdValid = lists.any { item.listId == it.id }
        if (isListIdValid) {
            listItems.remove(item)
        }
    }

    override fun getLibraryListsAsFlow(ownerId: Int): Flow<List<LibraryList>> {
        return flow { emit(getLibraryLists(ownerId)) }
    }

    override fun getLibraryListAsFlow(listId: String): Flow<LibraryList?> {
        return flow { emit(getLibraryList(listId)) }
    }

    override fun getListItemsAsFlow(listId: String): Flow<List<LibraryListItem>> {
        return flow { emit(getListItems(listId)) }
    }

    override suspend fun getLibraryLists(ownerId: Int): List<LibraryList> {
        return lists.filter { it.ownerId == ownerId }
    }

    override suspend fun getLibraryList(listId: String): LibraryList? {
        return lists.find { it.id == listId }
    }

    override suspend fun getListItems(listId: String): List<LibraryListItem> {
        return listItems.filter { it.listId == listId }
    }

    override suspend fun getListItem(id: Long): LibraryListItem? {
        return listItems.find { it.entryId == id }
    }

    override fun getListItemAsFlow(id: Long): Flow<LibraryListItem?> {
        return flow { emit(listItems.find { it.entryId == id }) }
    }
}
