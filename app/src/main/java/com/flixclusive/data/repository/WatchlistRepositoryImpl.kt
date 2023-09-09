package com.flixclusive.data.repository

import com.flixclusive.data.database.dao.WatchlistDao
import com.flixclusive.di.IoDispatcher
import com.flixclusive.domain.model.entities.WatchlistItem
import com.flixclusive.domain.repository.WatchlistRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

class WatchlistRepositoryImpl @Inject constructor(
    private val watchlistDao: WatchlistDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : WatchlistRepository {
    override suspend fun insert(item: WatchlistItem) = withContext(ioDispatcher) {
        watchlistDao.insert(item)
    }

    override suspend fun remove(item: WatchlistItem) = withContext(ioDispatcher) {
        watchlistDao.delete(item)
    }

    override suspend fun removeById(itemId: Int, ownerId: Int) = withContext(ioDispatcher) {
        watchlistDao.deleteById(itemId, ownerId)
    }

    override suspend fun getWatchlistItemById(filmId: Int, ownerId: Int): WatchlistItem? = withContext(ioDispatcher) {
        watchlistDao.getWatchlistItemById(filmId, ownerId)
    }

    override suspend fun getAllItems(ownerId: Int): List<WatchlistItem> = withContext(ioDispatcher) {
        watchlistDao.getAllItems(ownerId)
            ?.watchlist ?: emptyList()
    }

    override fun getAllItemsInFlow(ownerId: Int): Flow<List<WatchlistItem>> = watchlistDao.getAllItemsInFlow(ownerId)
        .map { it?.watchlist ?: emptyList() }
}