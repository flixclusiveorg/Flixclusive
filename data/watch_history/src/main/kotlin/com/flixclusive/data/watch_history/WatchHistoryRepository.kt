package com.flixclusive.data.watch_history

import com.flixclusive.model.database.WatchHistoryItem
import kotlinx.coroutines.flow.Flow

interface WatchHistoryRepository {
    fun getAllItemsInFlow(ownerId: Int): Flow<List<WatchHistoryItem>>

    suspend fun getWatchHistoryItemById(itemId: String, ownerId: Int): WatchHistoryItem?

    fun getWatchHistoryItemByIdInFlow(itemId: String, ownerId: Int): Flow<WatchHistoryItem?>

    suspend fun getRandomWatchHistoryItems(ownerId: Int, count: Int): List<WatchHistoryItem>

    suspend fun insert(item: WatchHistoryItem)

    suspend fun deleteById(itemId: String, ownerId: Int)

    suspend fun removeAll(ownerId: Int)
}