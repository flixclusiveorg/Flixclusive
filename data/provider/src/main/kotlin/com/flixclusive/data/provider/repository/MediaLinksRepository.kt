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
    ): List<CachedMediaLinksWithData>

    suspend fun getProviderLinks(
        ownerId: String,
        providerId: String,
        mediaId: String,
        episodeNumber: Int?,
        seasonNumber: Int?
    ): CachedMediaLinksWithData?

    suspend fun observeProviderLinks(
        ownerId: String,
        providerId: String,
        mediaId: String,
        episodeNumber: Int?,
        seasonNumber: Int?
    ): Flow<CachedMediaLinksWithData?>

    fun observeLinks(
        ownerId: String,
        mediaId: String,
        episodeNumber: Int?,
        seasonNumber: Int?
    ): Flow<List<CachedMediaLinksWithData>>

    suspend fun getById(id: String): CachedMediaLinksWithData?

    fun observeById(id: String): Flow<CachedMediaLinksWithData?>

    fun observeAll(ownerId: String): Flow<List<CachedMediaLinksWithData>>

    fun getSize(ownerId: String): Flow<Int>

    fun observeAllByMedia(ownerId: String, mediaId: String): Flow<List<CachedMediaLinksWithData>>

    suspend fun markLinkAsAlive(url: String, parentId: String)

    suspend fun markLinkAsDead(url: String, parentId: String)

    suspend fun deleteLink(link: DBMediaLink)

    suspend fun deleteLinks(links: List<DBMediaLink>)

    suspend fun deleteById(id: String)

    suspend fun deleteAll(ownerId: String)
}
