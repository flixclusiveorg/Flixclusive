package com.flixclusive.data.repository

import com.flixclusive.data.database.watch_history.WatchHistoryDao
import com.flixclusive.di.IoDispatcher
import com.flixclusive.domain.model.entities.WatchHistoryItem
import com.flixclusive.domain.repository.WatchHistoryRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject

class WatchHistoryRepositoryImpl @Inject constructor(
    private val watchHistoryDao: WatchHistoryDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : WatchHistoryRepository {

    override suspend fun getLatestItem(): WatchHistoryItem? = withContext(ioDispatcher) {
        watchHistoryDao.getLatestItem()
    }

    override suspend fun getAllItems(): List<WatchHistoryItem> = withContext(ioDispatcher) {
        watchHistoryDao.getAllItems()
    }

    override fun getAllItemsInFlow(): Flow<List<WatchHistoryItem>> = watchHistoryDao.getAllItemsInFlow()

    override suspend fun getWatchHistoryItemById(itemId: Int): WatchHistoryItem? = withContext(ioDispatcher) {
        watchHistoryDao.getWatchHistoryItemById(itemId)
    }

    override fun getWatchHistoryItemByIdInFlow(itemId: Int): Flow<WatchHistoryItem?> {
        return watchHistoryDao.getWatchHistoryItemByIdInFlow(itemId)
    }

    override suspend fun getRandomWatchHistoryItems(count: Int) = withContext(ioDispatcher) {
        watchHistoryDao.getRandomWatchHistoryItems(count = count)
    }

    override suspend fun insert(item: WatchHistoryItem) = withContext(ioDispatcher) {
        watchHistoryDao.insert(item)
    }

    override suspend fun deleteById(itemId: Int) = withContext(ioDispatcher) {
        watchHistoryDao.deleteById(itemId)
    }
}
