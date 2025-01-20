package com.flixclusive.data.library.custom

import com.flixclusive.data.library.custom.local.LibraryListDataSource
import com.flixclusive.data.tmdb.TMDBRepository
import com.flixclusive.model.database.LibraryList
import com.flixclusive.model.database.LibraryListItem
import com.flixclusive.model.film.Film
import kotlinx.coroutines.flow.Flow

internal class DefaultLibraryListRepository(
    private val tmdbRepository: TMDBRepository,
    private val dataSource: LibraryListDataSource,
) : LibraryListRepository {
    override suspend fun getLists(ownerId: Int): List<LibraryList> {
        return dataSource.getLibraryLists(ownerId)
    }

    override suspend fun createList(list: LibraryList) {
        dataSource.createList(list)
    }

    override suspend fun removeList(list: LibraryList) {
        dataSource.removeList(list)
    }

    override suspend fun getList(listId: String): LibraryList? {
        return dataSource.getLibraryList(listId)
    }

    override suspend fun observeList(listId: String): Flow<LibraryList?> {
        return dataSource.getLibraryListAsFlow(listId)
    }

    override fun observeLists(ownerId: Int): Flow<List<LibraryList>> {
        return dataSource.getLibraryListsAsFlow(ownerId)
    }

    override suspend fun addItemToList(item: LibraryListItem) {
        dataSource.addItemToList(item)
    }

    override suspend fun getListItem(id: Long): LibraryListItem? {
        return dataSource.getListItem(id)
    }

    override suspend fun observeListItem(id: Long): Flow<LibraryListItem?> {
        return dataSource.getListItemAsFlow(id)
    }

    override suspend fun removeItemFromList(item: LibraryListItem) {
        return dataSource.removeItemFromList(item)
    }

    override suspend fun getFilmsFromList(listId: String): List<Film> {
        TODO("Not yet implemented")
    }
}
