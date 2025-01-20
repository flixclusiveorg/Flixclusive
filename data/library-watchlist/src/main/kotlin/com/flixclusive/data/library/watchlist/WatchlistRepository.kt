package com.flixclusive.data.library.watchlist

import com.flixclusive.model.database.WatchlistItem
import kotlinx.coroutines.flow.Flow

interface WatchlistRepository {
    suspend fun insert(item: WatchlistItem)

    suspend fun remove(item: WatchlistItem)

    suspend fun removeAll(ownerId: Int)

    suspend fun removeById(itemId: String, ownerId: Int)

    suspend fun getWatchlistItemById(itemId: String, ownerId: Int): WatchlistItem?

    suspend fun getAllItems(ownerId: Int): List<WatchlistItem>

    fun getAllItemsInFlow(ownerId: Int): Flow<List<WatchlistItem>>
}
