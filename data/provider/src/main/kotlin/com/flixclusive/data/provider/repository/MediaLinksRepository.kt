package com.flixclusive.data.provider.repository

import com.flixclusive.core.database.entity.media.DBMedia
import com.flixclusive.core.database.entity.provider.CachedMediaLinks
import com.flixclusive.core.database.entity.provider.CachedMediaLinksWithData
import com.flixclusive.core.database.entity.provider.DBMediaLink
import kotlinx.coroutines.flow.Flow

interface MediaLinksRepository {
    suspend fun insertCache(
        entry: CachedMediaLinks,
        media: DBMedia? = null
    )
    suspend fun upsertLink(link: DBMediaLink)
    suspend fun getLinks(
        ownerId: String,
        mediaId: String,
        episodeNumber: Int?,
        seasonNumber: Int?
    ): CachedMediaLinksWithData?

    fun observeLinks(
        ownerId: String,
        mediaId: String,
        episodeNumber: Int?,
        seasonNumber: Int?
    ): Flow<CachedMediaLinksWithData?>

    suspend fun getLinksById(id: String): CachedMediaLinksWithData?
    fun observeLinksById(id: String): Flow<CachedMediaLinksWithData?>
    fun getAllAsFlow(ownerId: String): Flow<List<CachedMediaLinksWithData>>
    fun getCacheSize(ownerId: String): Flow<Int>
    fun getAllByMediaAsFlow(ownerId: String, mediaId: String): Flow<List<CachedMediaLinksWithData>>
    suspend fun markLinkAsAlive(url: String, parentId: String)
    suspend fun markLinkAsDead(url: String, parentId: String)
    suspend fun deleteCache(id: String)
    suspend fun deleteAll(ownerId: String)
}

