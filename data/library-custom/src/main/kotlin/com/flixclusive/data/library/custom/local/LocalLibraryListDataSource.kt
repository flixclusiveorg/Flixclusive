package com.flixclusive.data.library.custom.local

import com.flixclusive.core.database.dao.LibraryListCrossRefDao
import com.flixclusive.core.database.dao.LibraryListDao
import com.flixclusive.core.database.dao.LibraryListItemDao
import com.flixclusive.model.database.LibraryList
import com.flixclusive.model.database.LibraryListAndItemCrossRef
import com.flixclusive.model.database.LibraryListItem
import com.flixclusive.model.database.LibraryListItemWithLists
import com.flixclusive.model.database.LibraryListWithItems
import com.flixclusive.model.database.UserWithLibraryListsAndItems
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

internal class LocalLibraryListDataSource
    @Inject
    constructor(
        private val listDao: LibraryListDao,
        private val itemDao: LibraryListItemDao,
        private val crossRefDao: LibraryListCrossRefDao,
    ) : LibraryListDataSource {
        override fun getLists(userId: Int): Flow<List<LibraryList>> = listDao.getLists(userId)

        override fun getList(listId: Int): Flow<LibraryList?> = listDao.getList(listId)

        override fun getListWithItems(listId: Int): Flow<LibraryListWithItems?> = listDao.getListWithItems(listId)

        override suspend fun insertList(list: LibraryList) {
            listDao.insertList(list)
        }

        override suspend fun updateList(list: LibraryList) = listDao.updateList(list)

        override suspend fun deleteList(list: LibraryList) = listDao.deleteList(list)

        override suspend fun deleteListById(listId: Int) = listDao.deleteListById(listId)

        override fun getItem(itemId: String): Flow<LibraryListItem?> = itemDao.getItem(itemId)

        override fun getItemWithLists(itemId: String): Flow<LibraryListItemWithLists?> =
            itemDao.getItemWithLists(itemId)

        override suspend fun insertItem(item: LibraryListItem) {
            itemDao.insertItem(item)
        }

        override suspend fun updateItem(item: LibraryListItem) = itemDao.updateItem(item)

        override suspend fun deleteItem(item: LibraryListItem) = itemDao.deleteItem(item)

        override suspend fun insertCrossRef(crossRef: LibraryListAndItemCrossRef) = crossRefDao.insertCrossRef(crossRef)

        override suspend fun deleteCrossRef(crossRef: LibraryListAndItemCrossRef) = crossRefDao.deleteCrossRef(crossRef)

        override suspend fun deleteCrossRefById(
            listId: Int,
            itemId: String,
        ) = crossRefDao.deleteCrossRefById(listId, itemId)

        override fun getUserWithListsAndItems(userId: Int): Flow<UserWithLibraryListsAndItems?> =
            crossRefDao.getUserWithListsAndItems(userId)

        override fun getItemAddedDetails(
            listId: Int,
            itemId: String,
        ): Flow<LibraryListAndItemCrossRef?> = crossRefDao.getItemAddedDetails(listId, itemId)
    }
