package com.flixclusive.data.database.repository.impl

import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.database.dao.library.LibraryListDao
import com.flixclusive.core.database.dao.library.LibraryListItemDao
import com.flixclusive.core.database.entity.library.LibraryList
import com.flixclusive.core.database.entity.library.LibraryListItem
import com.flixclusive.core.database.entity.library.LibraryListItemWithMetadata
import com.flixclusive.core.database.entity.library.LibraryListWithItems
import com.flixclusive.data.database.repository.LibraryListRepository
import com.flixclusive.data.database.repository.LibrarySort
import com.flixclusive.model.film.Film
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject

internal class LibraryListRepositoryImpl @Inject constructor(
    private val listDao: LibraryListDao,
    private val itemDao: LibraryListItemDao,
    private val appDispatchers: AppDispatchers,
) : LibraryListRepository {
    override fun getLists(userId: Int): Flow<List<LibraryList>> {
        return listDao.getAllAsFlow(userId)
    }

    override fun getList(listId: Int): Flow<LibraryList?> {
        return listDao.getAsFlow(listId)
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
            listDao.deleteSafe(listId)
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
            itemDao.insert(item, film)
        }
    }

    override fun getListsContainingFilm(
        filmId: String,
        ownerId: Int,
    ): Flow<List<LibraryList>> {
        return listDao.getListsContainingFilmAsFlow(filmId, ownerId)
    }

    override suspend fun deleteItem(itemId: Long) {
        return itemDao.delete(itemId)
    }

    override fun searchItems(
        query: String,
        listId: Int,
        sort: LibrarySort
    ): Flow<List<LibraryListItemWithMetadata>> {
        val column = when (sort) {
            is LibrarySort.Added -> "item_createdAt"
            is LibrarySort.Modified -> "item_updatedAt"
            is LibrarySort.Name -> "film_title"
        }

        return itemDao.searchItems(
            query = query,
            listId = listId,
            columnSort = column,
            ascending = sort.ascending
        )
    }

    override fun getItems(listId: Int, sort: LibrarySort): Flow<List<LibraryListItemWithMetadata>> {
        val column = when (sort) {
            is LibrarySort.Added -> "item_createdAt"
            is LibrarySort.Modified -> "item_updatedAt"
            is LibrarySort.Name -> "film_title"
        }

        return itemDao.getByListId(
            listId = listId,
            columnSort = column,
            ascending = sort.ascending,
        )
    }

    override fun getListsAndItems(userId: Int, sort: LibrarySort): Flow<List<LibraryListWithItems>> {
        val column = when (sort) {
            is LibrarySort.Added -> "createdAt"
            is LibrarySort.Modified -> "updatedAt"
            is LibrarySort.Name -> "name"
        }

        return listDao.getLists(
            userId = userId,
            columnSort = column,
            ascending = sort.ascending,
        )
    }
}
