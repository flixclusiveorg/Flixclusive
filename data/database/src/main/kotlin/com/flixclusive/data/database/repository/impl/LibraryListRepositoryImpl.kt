package com.flixclusive.data.database.repository.impl

import android.content.Context
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.database.dao.library.LibraryListDao
import com.flixclusive.core.database.dao.library.LibraryListItemDao
import com.flixclusive.core.database.entity.library.LibraryList
import com.flixclusive.core.database.entity.library.LibraryListItem
import com.flixclusive.core.database.entity.library.LibraryListItemWithMetadata
import com.flixclusive.core.database.entity.library.LibraryListType
import com.flixclusive.core.database.entity.library.LibraryListWithItems
import com.flixclusive.data.database.repository.LibraryListRepository
import com.flixclusive.data.database.repository.LibrarySort
import com.flixclusive.model.film.Film
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.Date
import javax.inject.Inject
import com.flixclusive.core.database.R as DatabaseR

internal class LibraryListRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val listDao: LibraryListDao,
    private val itemDao: LibraryListItemDao,
    private val appDispatchers: AppDispatchers,
) : LibraryListRepository {
    override fun getLists(userId: String): Flow<List<LibraryList>> {
        return listDao.getAllAsFlow(userId)
    }

    override fun getList(listId: Int): Flow<LibraryList?> {
        return listDao.getAsFlow(listId)
    }

    override suspend fun insertList(list: LibraryList): Int {
        return withContext(appDispatchers.io) {
            listDao.insert(list.copy(updatedAt = Date())).toInt()
        }
    }

    override suspend fun updateList(list: LibraryList) {
        return withContext(appDispatchers.io) {
            listDao.update(list.copy(updatedAt = Date()))
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
            val list = listDao.get(item.listId)
            if (list != null) {
                listDao.update(list.copy(updatedAt = Date()))
            }

            itemDao.insert(item, film)
        }
    }

    override fun getListsContainingFilm(
        filmId: String,
        ownerId: String,
    ): Flow<List<LibraryList>> {
        return listDao.getListsContainingFilmAsFlow(filmId, ownerId)
    }

    override suspend fun deleteItem(itemId: Long) {
        return withContext(appDispatchers.io) {
            val item = itemDao.get(itemId) ?: return@withContext
            
            val list = listDao.get(item.item.listId)
            if (list != null) {
                listDao.update(list.copy(updatedAt = Date()))
            }

            itemDao.delete(itemId)
        }
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

    override fun getListsAndItems(userId: String, sort: LibrarySort): Flow<List<LibraryListWithItems>> {
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

    override suspend fun seedLists(userId: String) {
        val watchedListName = context.getString(DatabaseR.string.seeded_recently_watched)
        val watchedListDesc = context.getString(DatabaseR.string.seeded_recently_watched_description)

        withContext(appDispatchers.io) {
            listDao.insert(
                LibraryList(
                    name = watchedListName,
                    description = watchedListDesc,
                    ownerId = userId,
                    listType = LibraryListType.WATCHED,
                )
            )
        }
    }

    override suspend fun deleteAllExceptWatched(ownerId: String) {
        return withContext(appDispatchers.io) {
            listDao.deleteAllExceptWatched(ownerId)
        }
    }
}
