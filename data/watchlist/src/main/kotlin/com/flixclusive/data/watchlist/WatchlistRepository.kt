package com.flixclusive.data.watchlist

import com.flixclusive.model.database.WatchlistItem
import kotlinx.coroutines.flow.Flow

interface WatchlistRepository {
    suspend fun insert(item: WatchlistItem)

    suspend fun remove(item: WatchlistItem)

    suspend fun removeById(itemId: Int, ownerId: Int = 1)

    suspend fun getWatchlistItemById(filmId: Int, ownerId: Int = 1): WatchlistItem?

    suspend fun getAllItems(ownerId: Int = 1): List<WatchlistItem>

    fun getAllItemsInFlow(ownerId: Int = 1): Flow<List<WatchlistItem>>
}
