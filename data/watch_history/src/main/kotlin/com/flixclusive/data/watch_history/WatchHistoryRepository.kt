package com.flixclusive.data.watch_history

import com.flixclusive.model.database.WatchHistoryItem
import kotlinx.coroutines.flow.Flow

interface WatchHistoryRepository {
    fun getAllItemsInFlow(ownerId: Int = 1): Flow<List<WatchHistoryItem>>

    suspend fun getWatchHistoryItemById(itemId: Int, ownerId: Int = 1): WatchHistoryItem?

    fun getWatchHistoryItemByIdInFlow(itemId: Int, ownerId: Int = 1): Flow<WatchHistoryItem?>

    suspend fun getRandomWatchHistoryItems(ownerId: Int = 1, count: Int): List<WatchHistoryItem>

    suspend fun insert(item: WatchHistoryItem)

    suspend fun deleteById(itemId: Int, ownerId: Int = 1)
}