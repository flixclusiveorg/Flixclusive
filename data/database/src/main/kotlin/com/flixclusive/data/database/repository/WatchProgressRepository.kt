package com.flixclusive.data.database.repository

import com.flixclusive.core.database.entity.watched.EpisodeProgress
import com.flixclusive.core.database.entity.watched.WatchProgress
import com.flixclusive.core.database.entity.watched.WatchProgressWithMetadata
import com.flixclusive.model.film.Film
import com.flixclusive.model.film.util.FilmType
import kotlinx.coroutines.flow.Flow

interface WatchProgressRepository {
    fun getAllAsFlow(
        ownerId: String,
        sort: LibrarySort
    ): Flow<List<WatchProgressWithMetadata>>

    suspend fun get(
        id: Long,
        type: FilmType,
    ): WatchProgressWithMetadata?

    suspend fun get(
        id: String,
        ownerId: String,
        type: FilmType,
    ): WatchProgressWithMetadata?

    fun getAsFlow(
        id: Long,
        type: FilmType,
    ): Flow<WatchProgressWithMetadata?>

    fun getAsFlow(
        id: String,
        ownerId: String,
        type: FilmType,
    ): Flow<WatchProgressWithMetadata?>

    suspend fun getSeasonProgress(
        tvShowId: String,
        seasonNumber: Int,
        ownerId: String,
    ): List<EpisodeProgress>

    suspend fun getEpisodeProgress(
        tvShowId: String,
        seasonNumber: Int,
        episodeNumber: Int,
        ownerId: String,
    ): EpisodeProgress?

    fun getSeasonProgressAsFlow(
        tvShowId: String,
        seasonNumber: Int,
        ownerId: String,
    ): Flow<List<EpisodeProgress>>

    suspend fun getRandoms(
        ownerId: String,
        count: Int,
    ): Flow<List<WatchProgressWithMetadata>>

    suspend fun insert(item: WatchProgress, film: Film? = null): Long

    suspend fun delete(item: Long, type: FilmType)

    suspend fun deleteAll(ownerId: String)
}
