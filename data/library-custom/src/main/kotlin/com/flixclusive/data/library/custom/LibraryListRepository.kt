package com.flixclusive.data.library.custom

import com.flixclusive.model.database.LibraryList
import com.flixclusive.model.database.LibraryListItem
import com.flixclusive.model.film.Film
import kotlinx.coroutines.flow.Flow

interface LibraryListRepository {
    suspend fun getLists(ownerId: Int): List<LibraryList>

    fun observeLists(ownerId: Int): Flow<List<LibraryList>>

    suspend fun createList(list: LibraryList)

    suspend fun removeList(list: LibraryList)

    suspend fun getList(listId: String): LibraryList?

    suspend fun observeList(listId: String): Flow<LibraryList?>

    suspend fun addItemToList(item: LibraryListItem)

    suspend fun removeItemFromList(item: LibraryListItem)

    suspend fun getListItem(id: Long): LibraryListItem?

    suspend fun observeListItem(id: Long): Flow<LibraryListItem?>

    suspend fun getFilmsFromList(listId: String): List<Film>
}
