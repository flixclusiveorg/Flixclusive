package com.flixclusive.data.repository

import com.flixclusive.data.database.watchlist.WatchlistDao
import com.flixclusive.di.IoDispatcher
import com.flixclusive.domain.model.entities.WatchlistItem
import com.flixclusive.domain.repository.WatchlistRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
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

    override suspend fun removeById(itemId: Int) = withContext(ioDispatcher) {
        watchlistDao.deleteById(itemId = itemId)
    }

    override suspend fun getWatchlistItemById(filmId: Int): WatchlistItem? = withContext(ioDispatcher) {
        watchlistDao.getWatchlistItemById(filmId)
    }

    override suspend fun getAllItems(): List<WatchlistItem> = withContext(ioDispatcher) {
        watchlistDao.getAllItems()
    }

    override fun getAllItemsInFlow(): Flow<List<WatchlistItem>> {
        return watchlistDao.getAllItemsInFlow()
    }
}