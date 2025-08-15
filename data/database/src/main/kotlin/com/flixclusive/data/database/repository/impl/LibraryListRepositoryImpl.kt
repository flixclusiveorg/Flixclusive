package com.flixclusive.data.database.repository.impl

import com.flixclusive.core.database.entity.LibraryList
import com.flixclusive.core.database.entity.LibraryListAndItemCrossRef
import com.flixclusive.core.database.entity.LibraryListItem
import com.flixclusive.core.database.entity.LibraryListItemWithLists
import com.flixclusive.core.database.entity.LibraryListWithItems
import com.flixclusive.core.database.entity.UserWithLibraryListsAndItems
import com.flixclusive.data.database.datasource.LibraryListDataSource
import com.flixclusive.data.database.repository.LibraryListRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

internal class LibraryListRepositoryImpl
    @Inject
    constructor(
        private val dataSource: LibraryListDataSource,
    ) : LibraryListRepository {
        // Library List operations
        override fun getLists(userId: Int): Flow<List<LibraryList>> = dataSource.getLists(userId)

        override fun getList(listId: Int): Flow<LibraryList?> = dataSource.getList(listId)

        override fun getListWithItems(listId: Int): Flow<LibraryListWithItems?> = dataSource.getListWithItems(listId)

        override suspend fun insertList(list: LibraryList) = dataSource.insertList(list)

        override suspend fun updateList(list: LibraryList) = dataSource.updateList(list)

        override suspend fun deleteListById(listId: Int) {
            dataSource.deleteListById(listId)
        }

        // List Item operations
        override fun getItem(itemId: String): Flow<LibraryListItem?> = dataSource.getItem(itemId)

        override fun getItemWithLists(itemId: String): Flow<LibraryListItemWithLists?> =
            dataSource.getItemWithLists(itemId)

        override suspend fun addItemToList(
            listId: Int,
            item: LibraryListItem,
        ) = dataSource.addItemToList(listId, item)

        override suspend fun updateItem(item: LibraryListItem) = dataSource.updateItem(item)

        override suspend fun deleteItemFromList(
            listId: Int,
            itemId: String,
        ) {
            dataSource.deleteItemFromList(listId, itemId)
        }

        override fun getUserWithListsAndItems(userId: Int): Flow<UserWithLibraryListsAndItems> =
            dataSource.getUserWithListsAndItems(userId)

        override fun getCrossRef(
            listId: Int,
            itemId: String,
        ): Flow<LibraryListAndItemCrossRef?> = dataSource.getCrossRef(listId, itemId)
    }
