package com.flixclusive.data.provider.repository.impl

import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.database.dao.provider.CachedMediaLinkDao
import com.flixclusive.core.database.dao.provider.EpisodeLinks
import com.flixclusive.core.database.dao.provider.MediaLinksWithData
import com.flixclusive.core.database.dao.provider.SeasonLinks
import com.flixclusive.core.database.entity.media.DBMedia
import com.flixclusive.core.database.entity.provider.CachedMediaLink
import com.flixclusive.core.database.entity.provider.CachedStream
import com.flixclusive.core.database.entity.provider.CachedSubtitle
import com.flixclusive.data.provider.repository.MediaLinksRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject

internal class MediaLinksRepositoryImpl @Inject constructor(
    private val cachedMediaLinkDao: CachedMediaLinkDao,
    private val appDispatchers: AppDispatchers
) : MediaLinksRepository {
    override suspend fun upsertLinks(media: DBMedia, links: List<CachedMediaLink>) {
        withContext(appDispatchers.io) {
            cachedMediaLinkDao.upsertLinks(
                media = media,
                streams = links.filterIsInstance<CachedStream>(),
                subtitles = links.filterIsInstance<CachedSubtitle>()
            )
        }
    }

    override suspend fun upsertLink(link: CachedMediaLink) {
        withContext(appDispatchers.io) {
            when (link) {
                is CachedStream -> cachedMediaLinkDao.insertStream(link)
                is CachedSubtitle -> cachedMediaLinkDao.insertSubtitle(link)
            }
        }
    }

    override suspend fun getLinks(
        ownerId: String,
        mediaId: String,
        episodeNumber: Int?,
        seasonNumber: Int?
    ): List<MediaLinksWithData> = withContext(appDispatchers.io) {
        return@withContext cachedMediaLinkDao.getLinks(ownerId, mediaId, episodeNumber, seasonNumber)
    }

    override suspend fun getLinksByProvider(
        ownerId: String,
        providerId: String,
        mediaId: String,
        episodeNumber: Int?,
        seasonNumber: Int?
    ): MediaLinksWithData? = withContext(appDispatchers.io) {
        return@withContext cachedMediaLinkDao.getLinksByProvider(
            ownerId = ownerId,
            providerId = providerId,
            mediaId = mediaId,
            episodeNumber = episodeNumber,
            seasonNumber = seasonNumber
        )
    }

    override fun observeLinksByProvider(
        ownerId: String,
        providerId: String,
        mediaId: String,
        episodeNumber: Int?,
        seasonNumber: Int?
    ): Flow<MediaLinksWithData?> {
        return cachedMediaLinkDao.observeLinksByProvider(
            ownerId = ownerId,
            providerId = providerId,
            mediaId = mediaId,
            episodeNumber = episodeNumber,
            seasonNumber = seasonNumber
        )
    }

    override fun observeLinks(
        ownerId: String,
        mediaId: String,
        episodeNumber: Int?,
        seasonNumber: Int?
    ): Flow<List<MediaLinksWithData>> {
        return cachedMediaLinkDao.observeLinks(
            ownerId = ownerId,
            mediaId = mediaId,
            episodeNumber = episodeNumber,
            seasonNumber = seasonNumber
        )
    }

    override fun observeAll(ownerId: String): Flow<List<MediaLinksWithData>> {
        return cachedMediaLinkDao.observeAllMediaLinks(ownerId)
    }

    override fun getSize(ownerId: String): Flow<Int> = cachedMediaLinkDao.getCacheSize(ownerId)

    override fun observeCachedSeasons(mediaId: String, ownerId: String): Flow<List<SeasonLinks>> {
        return cachedMediaLinkDao.getCachedSeasons(mediaId, ownerId)
    }

    override fun observeCachedEpisodes(mediaId: String, seasonNumber: Int, ownerId: String): Flow<List<EpisodeLinks>> {
        return cachedMediaLinkDao.getCachedEpisodes(mediaId, seasonNumber, ownerId)
    }

    override suspend fun setLinkStatus(url: String, ownerId: String, isDead: Boolean) {
        withContext(appDispatchers.io) {
            cachedMediaLinkDao.setLinkStatus(url, ownerId, isDead)
        }
    }

    override suspend fun deleteLink(url: String, ownerId: String) {
        withContext(appDispatchers.io) {
            cachedMediaLinkDao.deleteLink(url, ownerId)
        }
    }

    override suspend fun deleteLinks(urls: List<String>, ownerId: String) {
        withContext(appDispatchers.io) {
            urls.forEach { cachedMediaLinkDao.deleteLink(it, ownerId) }
        }
    }

    override suspend fun deleteLinks(
        ownerId: String,
        mediaId: String,
        episodeNumber: Int?,
        seasonNumber: Int?
    ) {
        withContext(appDispatchers.io) {
            when {
                episodeNumber != null && seasonNumber != null ->
                    cachedMediaLinkDao.deleteLinksByEpisode(ownerId, mediaId, seasonNumber, episodeNumber)
                seasonNumber != null ->
                    cachedMediaLinkDao.deleteLinksBySeason(ownerId, mediaId, seasonNumber)
                else ->
                    cachedMediaLinkDao.deleteLinksByMedia(ownerId, mediaId)
            }
        }
    }

    override suspend fun deleteAll(ownerId: String) =
        withContext(appDispatchers.io) {
            cachedMediaLinkDao.deleteAll(ownerId)
        }
}
