package com.flixclusive.data.database.repository.impl

import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.database.dao.WatchlistDao
import com.flixclusive.core.database.entity.film.DBFilm.Companion.toDBFilm
import com.flixclusive.core.database.entity.watchlist.Watchlist
import com.flixclusive.core.database.entity.watchlist.WatchlistWithMetadata
import com.flixclusive.data.database.repository.WatchlistRepository
import com.flixclusive.model.film.Film
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject

internal class WatchlistRepositoryImpl
    @Inject
    constructor(
        private val watchlistDao: WatchlistDao,
        private val appDispatchers: AppDispatchers,
    ) : WatchlistRepository {
        override suspend fun insert(
            item: Watchlist,
            film: Film?,
        ): Long =
            withContext(appDispatchers.io) {
                watchlistDao.insert(item, film?.toDBFilm())
            }

        override suspend fun removeAll(ownerId: Int): Unit =
            withContext(appDispatchers.io) {
                watchlistDao.deleteAll(ownerId)
            }

        override suspend fun remove(id: Long): Unit =
            withContext(appDispatchers.io) {
                watchlistDao.delete(id)
            }

        override suspend fun get(
            filmId: String,
            ownerId: Int,
        ): WatchlistWithMetadata? {
            return withContext(appDispatchers.io) {
                watchlistDao.get(filmId, ownerId)
            }
        }

        override suspend fun get(id: Long): WatchlistWithMetadata? =
            withContext(appDispatchers.io) {
                watchlistDao.get(id)
            }

        override suspend fun getAll(ownerId: Int): List<WatchlistWithMetadata> =
            withContext(appDispatchers.io) {
                watchlistDao.getAll(ownerId)
            }

        override fun getAllAsFlow(ownerId: Int): Flow<List<WatchlistWithMetadata>> = watchlistDao.getAllAsFlow(ownerId)
    }
