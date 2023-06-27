package com.flixclusive.domain.repository

import com.flixclusive.domain.model.entities.WatchlistItem
import kotlinx.coroutines.flow.Flow

interface WatchlistRepository {
    suspend fun insert(item: WatchlistItem)

    suspend fun remove(item: WatchlistItem)

    suspend fun removeById(itemId: Int)

    suspend fun getWatchlistItemById(filmId: Int): WatchlistItem?

    suspend fun getAllItems(): List<WatchlistItem>

    fun getAllItemsInFlow(): Flow<List<WatchlistItem>>
}
