package com.flixclusive.data.database.repository

import com.flixclusive.core.database.entity.watched.EpisodeProgress
import com.flixclusive.core.database.entity.watched.WatchProgress
import com.flixclusive.core.database.entity.watched.WatchProgressWithMetadata
import com.flixclusive.model.film.Film
import com.flixclusive.model.film.util.FilmType
import kotlinx.coroutines.flow.Flow

interface WatchProgressRepository {
    fun getAllAsFlow(ownerId: Int): Flow<List<WatchProgressWithMetadata>>

    suspend fun get(
        id: Long,
        type: FilmType,
    ): WatchProgressWithMetadata?

    suspend fun get(
        id: String,
        ownerId: Int,
        type: FilmType,
    ): WatchProgressWithMetadata?

    fun getAsFlow(
        id: Long,
        type: FilmType,
    ): Flow<WatchProgressWithMetadata?>

    fun getAsFlow(
        id: String,
        ownerId: Int,
        type: FilmType,
    ): Flow<WatchProgressWithMetadata?>

    suspend fun getSeasonProgress(
        tvShowId: String,
        seasonNumber: Int,
        ownerId: Int,
    ): List<EpisodeProgress>

    suspend fun getRandoms(
        ownerId: Int,
        count: Int,
    ): Flow<List<WatchProgressWithMetadata>>

    suspend fun insert(item: WatchProgress, film: Film? = null): Long

    suspend fun delete(item: Long, type: FilmType)

    suspend fun removeAll(ownerId: Int)
}
