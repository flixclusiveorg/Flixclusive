package com.flixclusive.data.database.repository

import com.flixclusive.core.database.entity.watched.EpisodeProgress
import com.flixclusive.core.database.entity.watched.WatchProgress
import com.flixclusive.core.database.entity.watched.WatchProgressWithMetadata
import com.flixclusive.model.media.MediaMetadata
import com.flixclusive.model.media.common.MediaType
import kotlinx.coroutines.flow.Flow

interface WatchProgressRepository {
    fun getAllAsFlow(
        ownerId: String,
        sort: LibrarySort
    ): Flow<List<WatchProgressWithMetadata>>

    suspend fun get(
        id: Long,
        type: MediaType,
    ): WatchProgressWithMetadata?

    suspend fun get(
        id: String,
        ownerId: String,
        type: MediaType,
    ): WatchProgressWithMetadata?

    fun getAsFlow(
        id: Long,
        type: MediaType,
    ): Flow<WatchProgressWithMetadata?>

    fun getAsFlow(
        id: String,
        ownerId: String,
        type: MediaType,
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

    suspend fun insert(item: WatchProgress, media: MediaMetadata? = null): Long

    suspend fun delete(item: Long, type: MediaType)

    suspend fun deleteAll(ownerId: String)
}
