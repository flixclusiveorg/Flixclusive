package com.flixclusive.data.database.repository

import com.flixclusive.core.database.entity.library.LibraryList
import com.flixclusive.core.database.entity.library.LibraryListItem
import com.flixclusive.core.database.entity.library.LibraryListItemWithMetadata
import com.flixclusive.core.database.entity.library.LibraryListWithItems
import com.flixclusive.core.database.entity.library.UserWithLibraryListsAndItems
import com.flixclusive.model.film.Film
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing library lists and items.
 *
 * Provides methods to interact with library lists, items, and their metadata.
 * */
interface LibraryListRepository {
    fun getLists(userId: Int): Flow<List<LibraryList>>

    fun getList(listId: Int): Flow<LibraryList?>

    suspend fun insertList(list: LibraryList): Int

    suspend fun updateList(list: LibraryList)

    suspend fun deleteListById(listId: Int)

    fun getItemAsFlow(itemId: Long): Flow<LibraryListItemWithMetadata?>

    suspend fun getItem(itemId: Long): LibraryListItemWithMetadata?

    suspend fun insertItem(item: LibraryListItem, film: Film? = null): Long

    fun getListsContainingFilm(filmId: String, ownerId: Int): Flow<List<LibraryList>>

    suspend fun deleteItem(itemId: Long)

    fun getListWithItems(listId: Int): Flow<LibraryListWithItems?>

    fun getUserWithListsAndItems(userId: Int): Flow<UserWithLibraryListsAndItems>
}
