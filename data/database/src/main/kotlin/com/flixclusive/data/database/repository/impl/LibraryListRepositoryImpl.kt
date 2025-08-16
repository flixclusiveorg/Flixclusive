package com.flixclusive.data.database.repository.impl

import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.database.dao.LibraryListDao
import com.flixclusive.core.database.dao.LibraryListItemDao
import com.flixclusive.core.database.entity.film.DBFilm.Companion.toDBFilm
import com.flixclusive.core.database.entity.library.LibraryList
import com.flixclusive.core.database.entity.library.LibraryListItem
import com.flixclusive.core.database.entity.library.LibraryListItemWithMetadata
import com.flixclusive.core.database.entity.library.LibraryListWithItems
import com.flixclusive.core.database.entity.library.UserWithLibraryListsAndItems
import com.flixclusive.data.database.repository.LibraryListRepository
import com.flixclusive.model.film.Film
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject

internal class LibraryListRepositoryImpl
    @Inject
    constructor(
        private val listDao: LibraryListDao,
        private val itemDao: LibraryListItemDao,
        private val appDispatchers: AppDispatchers,
    ) : LibraryListRepository {
        override fun getLists(userId: Int): Flow<List<LibraryList>> {
            return listDao.getAll(userId)
        }

        override fun getList(listId: Int): Flow<LibraryList?> {
            return listDao.get(listId)
        }

        override suspend fun insertList(list: LibraryList): Int {
            return withContext(appDispatchers.io) {
                listDao.insert(list).toInt()
            }
        }

        override suspend fun updateList(list: LibraryList) {
            return withContext(appDispatchers.io) {
                listDao.update(list)
            }
        }

        override suspend fun deleteListById(listId: Int) {
            return withContext(appDispatchers.io) {
                listDao.delete(listId)
            }
        }

        override fun getItemAsFlow(itemId: Long): Flow<LibraryListItemWithMetadata?> {
            return itemDao.getAsFlow(itemId)
        }

        override suspend fun getItem(itemId: Long): LibraryListItemWithMetadata? {
            return withContext(appDispatchers.io) {
                itemDao.get(itemId)
            }
        }

        override suspend fun insertItem(
            item: LibraryListItem,
            film: Film?,
        ): Long {
            return withContext(appDispatchers.io) {
                itemDao.insert(item, film?.toDBFilm())
            }
        }

        override fun getListsContainingFilm(
            filmId: String,
            ownerId: Int,
        ): Flow<List<LibraryList>> {
            return listDao.getListsContainingFilm(filmId, ownerId)
        }

        override suspend fun deleteItem(itemId: Long) {
            return itemDao.delete(itemId)
        }

        override fun getListWithItems(listId: Int): Flow<LibraryListWithItems?> {
            return listDao.getListWithItems(listId)
        }

        override fun getUserWithListsAndItems(userId: Int): Flow<UserWithLibraryListsAndItems> {
            return listDao.getUserWithListsAndItems(userId)
        }
    }
