package com.flixclusive.domain.repository

import com.flixclusive.domain.model.entities.WatchHistoryItem
import kotlinx.coroutines.flow.Flow

interface WatchHistoryRepository {
    suspend fun getLatestItem(): WatchHistoryItem?

    suspend fun getAllItems(): List<WatchHistoryItem>

    fun getAllItemsInFlow(): Flow<List<WatchHistoryItem>>

    suspend fun getWatchHistoryItemById(itemId: Int): WatchHistoryItem?

    fun getWatchHistoryItemByIdInFlow(itemId: Int): Flow<WatchHistoryItem?>

    suspend fun getRandomWatchHistoryItems(count: Int): List<WatchHistoryItem>

    suspend fun insert(item: WatchHistoryItem)

    suspend fun deleteById(itemId: Int)
}