package com.flixclusive.data.watch_history

import com.flixclusive.core.database.dao.WatchHistoryDao
import com.flixclusive.core.util.coroutines.AppDispatchers.Companion.withIOContext
import com.flixclusive.model.database.WatchHistoryItem
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

internal class DefaultWatchHistoryRepository @Inject constructor(
    private val watchHistoryDao: WatchHistoryDao
) : WatchHistoryRepository {

    override fun getAllItemsInFlow(ownerId: Int): Flow<List<WatchHistoryItem>> = watchHistoryDao
        .getAllItemsInFlow(ownerId)

    override suspend fun getWatchHistoryItemById(itemId: String, ownerId: Int): WatchHistoryItem? = withIOContext {
        watchHistoryDao.getWatchHistoryItemById(itemId, ownerId)
    }

    override fun getWatchHistoryItemByIdInFlow(itemId: String, ownerId: Int): Flow<WatchHistoryItem?> {
        return watchHistoryDao.getWatchHistoryItemByIdInFlow(itemId, ownerId)
    }

    override suspend fun getRandomWatchHistoryItems(ownerId: Int, count: Int) = withIOContext {
        watchHistoryDao.getRandomItems(ownerId, count)
    }

    override suspend fun insert(item: WatchHistoryItem) = withIOContext {
        watchHistoryDao.insert(item)
    }

    override suspend fun deleteById(itemId: String, ownerId: Int) = withIOContext {
        watchHistoryDao.deleteById(itemId, ownerId)
    }
}
