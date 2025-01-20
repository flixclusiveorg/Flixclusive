package com.flixclusive.data.library.custom.local

import com.flixclusive.core.database.dao.LibraryListDao
import com.flixclusive.core.util.coroutines.AppDispatchers.Companion.withIOContext
import com.flixclusive.model.database.LibraryList
import com.flixclusive.model.database.LibraryListItem
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

internal class LocalLibraryListDataSource
    @Inject
    constructor(
        private val libraryListDao: LibraryListDao,
    ) : LibraryListDataSource {
        override suspend fun createList(list: LibraryList) = withIOContext { libraryListDao.createList(list) }

        override suspend fun removeList(list: LibraryList) = withIOContext { libraryListDao.removeList(list) }

        override suspend fun addItemToList(item: LibraryListItem) = withIOContext { libraryListDao.addItemToList(item) }

        override suspend fun removeItemFromList(item: LibraryListItem) =
            withIOContext { libraryListDao.removeItemFromList(item) }

        override fun getLibraryListsAsFlow(ownerId: Int): Flow<List<LibraryList>> =
            libraryListDao.getLibraryListsAsFlow(ownerId)

        override fun getLibraryListAsFlow(listId: String): Flow<LibraryList?> =
            libraryListDao.getLibraryListAsFlow(listId)

        override fun getListItemsAsFlow(listId: String): Flow<List<LibraryListItem>> =
            libraryListDao.getListItemsAsFlow(listId)

        override suspend fun getLibraryLists(ownerId: Int): List<LibraryList> =
            withIOContext { libraryListDao.getLibraryLists(ownerId) }

        override suspend fun getLibraryList(listId: String): LibraryList? =
            withIOContext { libraryListDao.getLibraryList(listId) }

        override suspend fun getListItems(listId: String): List<LibraryListItem> =
            withIOContext { libraryListDao.getListItems(listId) }

        override suspend fun getListItem(id: Long): LibraryListItem? {
            return withIOContext { libraryListDao.getListItem(id) }
        }

        override fun getListItemAsFlow(id: Long): Flow<LibraryListItem?> {
            return libraryListDao.getListItemAsFlow(id)
        }
    }
