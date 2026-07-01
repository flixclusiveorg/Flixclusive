package com.flixclusive.data.provider.repository

import com.flixclusive.core.database.entity.provider.CachedMediaLink
import com.flixclusive.core.database.entity.provider.EpisodeLinks
import com.flixclusive.core.database.entity.provider.MediaLinksWithData
import com.flixclusive.core.database.entity.provider.SeasonLinks
import com.flixclusive.model.media.MediaMetadata
import kotlinx.coroutines.flow.Flow

interface MediaLinksRepository {
    suspend fun upsertLink(link: CachedMediaLink)

    suspend fun upsertMedia(media: MediaMetadata)

    suspend fun getLinks(
        ownerId: String,
        mediaId: String,
        episodeNumber: Int?,
        seasonNumber: Int?
    ): List<MediaLinksWithData>

    suspend fun getLinksByProvider(
        ownerId: String,
        providerId: String,
        mediaId: String,
        episodeNumber: Int?,
        seasonNumber: Int?
    ): MediaLinksWithData?

    fun observeLinksByProvider(
        ownerId: String,
        providerId: String,
        mediaId: String,
        episodeNumber: Int?,
        seasonNumber: Int?
    ): Flow<MediaLinksWithData?>

    fun observeLinks(
        ownerId: String,
        mediaId: String,
        episodeNumber: Int?,
        seasonNumber: Int?
    ): Flow<List<MediaLinksWithData>>

    fun observeAll(ownerId: String): Flow<List<MediaLinksWithData>>

    fun getSize(ownerId: String): Flow<Int>

    fun observeCachedSeasons(mediaId: String, ownerId: String): Flow<List<SeasonLinks>>

    fun observeCachedEpisodes(mediaId: String, seasonNumber: Int, ownerId: String): Flow<List<EpisodeLinks>>

    suspend fun setLinkStatus(url: String, ownerId: String, isDead: Boolean)

    suspend fun deleteLink(url: String, ownerId: String)

    suspend fun deleteLinks(urls: List<String>, ownerId: String)

    suspend fun deleteLinks(
        ownerId: String,
        mediaId: String,
        episodeNumber: Int?,
        seasonNumber: Int?
    )

    suspend fun deleteAll(ownerId: String)
}
