package com.flixclusive.data.library.custom.local

import android.database.sqlite.SQLiteConstraintException
import com.flixclusive.core.database.dao.LibraryListAndItemDao
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
        private val crossRefDao: LibraryListAndItemDao,
    ) : LibraryListDataSource {
        override fun getLists(userId: Int): Flow<List<LibraryList>> = listDao.getLists(userId)

        override fun getList(listId: Int): Flow<LibraryList?> = listDao.getList(listId)

        override suspend fun insertList(list: LibraryList) {
            listDao.insertList(list)
        }

        override suspend fun updateList(list: LibraryList) = listDao.updateList(list)

        override suspend fun deleteListById(listId: Int) = listDao.deleteListById(listId)

        override fun getItem(itemId: String): Flow<LibraryListItem?> = itemDao.getItem(itemId)

        override suspend fun updateItem(item: LibraryListItem) = itemDao.updateItem(item)

        override suspend fun addItemToList(
            listId: Int,
            item: LibraryListItem,
        ) {
            itemDao.insertItem(item)

            val crossRef =
                LibraryListAndItemCrossRef(
                    listId = listId,
                    itemId = item.id,
                )
            crossRefDao.addItemToList(crossRef)
        }

        override suspend fun deleteItemFromList(
            listId: Int,
            itemId: String,
        ) {
            crossRefDao.deleteItemFromList(listId = listId, itemId = itemId)
            tryDeletingItemFromDatabase(itemId)
        }

        override fun getItemWithLists(itemId: String): Flow<LibraryListItemWithLists?> =
            crossRefDao.getItemWithLists(itemId)

        override fun getListWithItems(listId: Int): Flow<LibraryListWithItems?> = crossRefDao.getListWithItems(listId)

        override fun getUserWithListsAndItems(userId: Int): Flow<UserWithLibraryListsAndItems> =
            crossRefDao.getUserWithListsAndItems(userId)

        override fun getCrossRef(
            listId: Int,
            itemId: String,
        ): Flow<LibraryListAndItemCrossRef?> = crossRefDao.getCrossRef(listId, itemId)

        private suspend fun tryDeletingItemFromDatabase(itemId: String) {
            try {
                itemDao.deleteItemById(itemId = itemId)
            } catch (_: SQLiteConstraintException) {
            }
        }
    }
