package com.flixclusive.data.database.repository

import com.flixclusive.core.database.entity.WatchHistory
import kotlinx.coroutines.flow.Flow

interface WatchHistoryRepository {
    fun getAllItemsInFlow(ownerId: Int): Flow<List<WatchHistory>>

    suspend fun getWatchHistoryItemById(itemId: String, ownerId: Int): WatchHistory?

    fun getWatchHistoryItemByIdInFlow(itemId: String, ownerId: Int): Flow<WatchHistory?>

    suspend fun getRandomWatchHistoryItems(ownerId: Int, count: Int): Flow<List<WatchHistory>>

    suspend fun insert(item: WatchHistory)

    suspend fun deleteById(itemId: String, ownerId: Int)

    suspend fun removeAll(ownerId: Int)
}
