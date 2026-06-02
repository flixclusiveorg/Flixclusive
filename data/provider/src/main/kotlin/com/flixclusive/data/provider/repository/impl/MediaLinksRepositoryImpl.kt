package com.flixclusive.data.provider.repository.impl

import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.database.dao.provider.CachedMediaLinksDao
import com.flixclusive.core.database.dao.provider.DBMediaLinkDao
import com.flixclusive.core.database.entity.media.DBMedia
import com.flixclusive.core.database.entity.provider.CachedMediaLinks
import com.flixclusive.core.database.entity.provider.CachedMediaLinksWithData
import com.flixclusive.core.database.entity.provider.DBMediaLink
import com.flixclusive.core.database.entity.provider.DBStream
import com.flixclusive.core.database.entity.provider.DBSubtitle
import com.flixclusive.data.provider.repository.MediaLinksRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject

internal class MediaLinksRepositoryImpl @Inject constructor(
    private val cachedMediaLinksDao: CachedMediaLinksDao,
    private val dbMediaLinkDao: DBMediaLinkDao,
    private val appDispatchers: AppDispatchers
) : MediaLinksRepository {

    override suspend fun insertCache(entry: CachedMediaLinks, media: DBMedia?) =
        withContext(appDispatchers.io) {
            cachedMediaLinksDao.insertCache(entry, media)
        }

    override suspend fun upsertLink(link: DBMediaLink) {
        withContext(appDispatchers.io) {
            when (link) {
                is DBStream -> dbMediaLinkDao.upsertStream(link)
                is DBSubtitle -> dbMediaLinkDao.upsertSubtitle(link)
            }
        }
    }

    override suspend fun getLinks(ownerId: String, mediaId: String, episodeNumber: Int?, seasonNumber: Int?): CachedMediaLinksWithData? =
        withContext(appDispatchers.io) {
            cachedMediaLinksDao.getByKey(ownerId, mediaId, episodeNumber, seasonNumber)
        }

    override fun observeLinks(ownerId: String, mediaId: String, episodeNumber: Int?, seasonNumber: Int?): Flow<CachedMediaLinksWithData?> =
        cachedMediaLinksDao.getByKeyAsFlow(ownerId, mediaId, episodeNumber, seasonNumber)

    override suspend fun getLinksById(id: String): CachedMediaLinksWithData? =
        withContext(appDispatchers.io) {
            cachedMediaLinksDao.getById(id)
        }

    override fun observeLinksById(id: String): Flow<CachedMediaLinksWithData?> =
        cachedMediaLinksDao.getByIdAsFlow(id)

    override fun getAllAsFlow(ownerId: String): Flow<List<CachedMediaLinksWithData>> =
        cachedMediaLinksDao.getAllAsFlow(ownerId)

    override fun getCacheSize(ownerId: String): Flow<Int>
        = cachedMediaLinksDao.getCacheSize(ownerId)

    override fun getAllByMediaAsFlow(ownerId: String, mediaId: String): Flow<List<CachedMediaLinksWithData>> =
        cachedMediaLinksDao.getAllByMediaAsFlow(ownerId, mediaId)

    override suspend fun markLinkAsAlive(url: String, parentId: String) =
        withContext(appDispatchers.io) {
            dbMediaLinkDao.markLinkAsAlive(url, parentId)
        }

    override suspend fun markLinkAsDead(url: String, parentId: String) {
        withContext(appDispatchers.io) {
            dbMediaLinkDao.markLinkAsDead(url, parentId)
        }
    }

    override suspend fun deleteCache(id: String) =
        withContext(appDispatchers.io) {
            cachedMediaLinksDao.delete(id)
        }

    override suspend fun deleteAll(ownerId: String) =
        withContext(appDispatchers.io) {
            cachedMediaLinksDao.deleteAll(ownerId)
        }
}

