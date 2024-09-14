package com.flixclusive.data.watchlist

import com.flixclusive.core.database.dao.WatchlistDao
import com.flixclusive.core.util.coroutines.AppDispatchers.Companion.withIOContext
import com.flixclusive.model.database.WatchlistItem
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

internal class DefaultWatchlistRepository @Inject constructor(
    private val watchlistDao: WatchlistDao
) : WatchlistRepository {
    override suspend fun insert(item: WatchlistItem) = withIOContext {
        watchlistDao.insert(item)
    }

    override suspend fun remove(item: WatchlistItem) = withIOContext {
        watchlistDao.delete(item)
    }

    override suspend fun removeById(itemId: String, ownerId: Int) = withIOContext {
        watchlistDao.deleteById(itemId, ownerId)
    }

    override suspend fun getWatchlistItemById(itemId: String, ownerId: Int): WatchlistItem? = withIOContext {
        watchlistDao.getWatchlistItemById(itemId, ownerId)
    }

    override suspend fun getAllItems(ownerId: Int): List<WatchlistItem> = withIOContext {
        watchlistDao.getAllItems(ownerId)
    }

    override fun getAllItemsInFlow(ownerId: Int): Flow<List<WatchlistItem>> = watchlistDao.getAllItemsInFlow(ownerId)
}