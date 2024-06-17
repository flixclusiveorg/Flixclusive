package com.flixclusive.data.watch_history

import com.flixclusive.core.database.dao.WatchHistoryDao
import com.flixclusive.core.util.common.dispatcher.AppDispatchers
import com.flixclusive.core.util.common.dispatcher.Dispatcher
import com.flixclusive.model.database.WatchHistoryItem
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject

internal class DefaultWatchHistoryRepository @Inject constructor(
    private val watchHistoryDao: WatchHistoryDao,
    @Dispatcher(AppDispatchers.IO) private val ioDispatcher: CoroutineDispatcher
) : WatchHistoryRepository {

    override fun getAllItemsInFlow(ownerId: Int): Flow<List<WatchHistoryItem>> = watchHistoryDao
        .getAllItemsInFlow(ownerId)

    override suspend fun getWatchHistoryItemById(itemId: String, ownerId: Int): WatchHistoryItem? = withContext(ioDispatcher) {
        watchHistoryDao.getWatchHistoryItemById(itemId, ownerId)
    }

    override fun getWatchHistoryItemByIdInFlow(itemId: String, ownerId: Int): Flow<WatchHistoryItem?> {
        return watchHistoryDao.getWatchHistoryItemByIdInFlow(itemId, ownerId)
    }

    override suspend fun getRandomWatchHistoryItems(ownerId: Int, count: Int) = withContext(ioDispatcher) {
        watchHistoryDao.getRandomItems(ownerId, count)
    }

    override suspend fun insert(item: WatchHistoryItem) = withContext(ioDispatcher) {
        watchHistoryDao.insert(item)
    }

    override suspend fun deleteById(itemId: String, ownerId: Int) = withContext(ioDispatcher) {
        watchHistoryDao.deleteById(itemId, ownerId)
    }
}
