package com.flixclusive.data.repository

import com.flixclusive.data.database.dao.WatchHistoryDao
import com.flixclusive.di.IoDispatcher
import com.flixclusive.domain.model.entities.WatchHistoryItem
import com.flixclusive.domain.repository.WatchHistoryRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

class WatchHistoryRepositoryImpl @Inject constructor(
    private val watchHistoryDao: WatchHistoryDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : WatchHistoryRepository {

    override fun getAllItemsInFlow(ownerId: Int): Flow<List<WatchHistoryItem>> = watchHistoryDao.getAllItemsInFlow(ownerId)
        .map { it?.watchHistory ?: emptyList() }

    override suspend fun getWatchHistoryItemById(itemId: Int, ownerId: Int): WatchHistoryItem? = withContext(ioDispatcher) {
        watchHistoryDao.getWatchHistoryItemById(itemId, ownerId)
    }

    override fun getWatchHistoryItemByIdInFlow(itemId: Int, ownerId: Int): Flow<WatchHistoryItem?> {
        return watchHistoryDao.getWatchHistoryItemByIdInFlow(itemId, ownerId)
    }

    override suspend fun getRandomWatchHistoryItems(ownerId: Int, count: Int) = withContext(ioDispatcher) {
        return@withContext watchHistoryDao.getRandomItems(ownerId, count)
            ?.watchHistory ?: return@withContext emptyList()
    }

    override suspend fun insert(item: WatchHistoryItem) = withContext(ioDispatcher) {
        watchHistoryDao.insert(item)
    }

    override suspend fun deleteById(itemId: Int, ownerId: Int) = withContext(ioDispatcher) {
        watchHistoryDao.deleteById(itemId, ownerId)
    }
}
