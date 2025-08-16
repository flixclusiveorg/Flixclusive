package com.flixclusive.data.database.repository

import com.flixclusive.core.database.entity.watchlist.Watchlist
import com.flixclusive.core.database.entity.watchlist.WatchlistWithMetadata
import com.flixclusive.model.film.Film
import kotlinx.coroutines.flow.Flow

interface WatchlistRepository {
    suspend fun insert(item: Watchlist, film: Film? = null): Long

    suspend fun removeAll(ownerId: Int)

    suspend fun remove(id: Long)

    suspend fun get(id: Long): WatchlistWithMetadata?

    suspend fun isInWatchlist(filmId: String, ownerId: Int): Boolean

    suspend fun getAll(ownerId: Int): List<WatchlistWithMetadata>

    fun getAllAsFlow(ownerId: Int): Flow<List<WatchlistWithMetadata>>
}
