package com.flixclusive.data.database.repository

import com.flixclusive.core.database.entity.library.LibraryList
import com.flixclusive.core.database.entity.library.LibraryListItem
import com.flixclusive.core.database.entity.library.LibraryListItemWithMetadata
import com.flixclusive.core.database.entity.library.LibraryListWithItems
import com.flixclusive.model.media.MediaMetadata
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing library lists and items.
 *
 * Provides methods to interact with library lists, items, and their metadata.
 * */
interface LibraryListRepository {
    fun getLists(userId: String): Flow<List<LibraryList>>

    fun getList(listId: String): Flow<LibraryList?>

    suspend fun insertList(list: LibraryList): String

    suspend fun updateList(list: LibraryList)

    suspend fun deleteListById(listId: String)

    fun getItemAsFlow(itemId: String): Flow<LibraryListItemWithMetadata?>

    suspend fun getItem(itemId: String): LibraryListItemWithMetadata?

    suspend fun insertItem(item: LibraryListItem, media: MediaMetadata? = null): String

    fun getListsContainingMedia(mediaId: String, ownerId: String): Flow<List<LibraryList>>

    suspend fun isInLibrary(mediaId: String, ownerId: String): Boolean

    suspend fun deleteItem(itemId: String)

    suspend fun paginateItems(
        listId: String,
        sort: LibrarySort,
        pageSize: Int,
        page: Int,
    ): List<LibraryListItemWithMetadata>

    fun getListsAndItems(userId: String, sort: LibrarySort): Flow<List<LibraryListWithItems>>

    fun searchItems(query: String, listId: String, sort: LibrarySort): Flow<List<LibraryListItemWithMetadata>>

    suspend fun deleteAllExceptWatched(ownerId: String)

    suspend fun seedLists(userId: String)
}
