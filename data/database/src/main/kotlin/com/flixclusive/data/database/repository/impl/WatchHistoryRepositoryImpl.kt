package com.flixclusive.data.database.repository.impl

import com.flixclusive.core.database.dao.WatchHistoryDao
import com.flixclusive.core.database.entity.WatchHistory
import com.flixclusive.core.util.coroutines.AppDispatchers.Companion.withIOContext
import com.flixclusive.data.database.repository.WatchHistoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

internal class WatchHistoryRepositoryImpl @Inject constructor(
    private val watchHistoryDao: WatchHistoryDao
) : WatchHistoryRepository {

    override fun getAllItemsInFlow(ownerId: Int): Flow<List<WatchHistory>> = watchHistoryDao
        .getAllItemsInFlow(ownerId)

    override suspend fun getWatchHistoryItemById(itemId: String, ownerId: Int): WatchHistory? = withIOContext {
        watchHistoryDao.getWatchHistoryItemById(itemId, ownerId)
    }

    override fun getWatchHistoryItemByIdInFlow(itemId: String, ownerId: Int): Flow<WatchHistory?> {
        return watchHistoryDao.getWatchHistoryItemByIdInFlow(itemId, ownerId)
    }

    override suspend fun getRandomWatchHistoryItems(ownerId: Int, count: Int) = withIOContext {
        watchHistoryDao.getRandomItems(ownerId, count)
    }

    override suspend fun insert(item: WatchHistory) = withIOContext {
        watchHistoryDao.insert(item)
    }

    override suspend fun deleteById(itemId: String, ownerId: Int) = withIOContext {
        watchHistoryDao.deleteById(itemId, ownerId)
    }

    override suspend fun removeAll(ownerId: Int) {
        watchHistoryDao.deleteAll(ownerId)
    }
}
