package com.flixclusive.data.backup.restore.impl

import com.flixclusive.core.database.dao.provider.CachedMediaLinkDao
import com.flixclusive.core.database.entity.media.DBMedia
import com.flixclusive.core.database.entity.media.DBMediaExternalId
import com.flixclusive.core.database.entity.media.DBMediaFts
import com.flixclusive.core.database.entity.provider.CachedStream
import com.flixclusive.core.database.entity.provider.CachedSubtitle
import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.data.backup.model.BackupCachedLink
import com.flixclusive.data.backup.model.BackupCachedStream
import com.flixclusive.data.backup.model.BackupCachedSubtitle
import com.flixclusive.data.backup.model.BackupDbMedia
import com.flixclusive.data.backup.model.BackupDbMediaExternalId
import com.flixclusive.data.backup.restore.BackupRestorer
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import java.util.Date
import javax.inject.Inject

internal class CachedLinkBackupRestorer @Inject constructor(
    private val cachedMediaLinkDao: CachedMediaLinkDao,
    private val userSessionDataStore: UserSessionDataStore,
) : BackupRestorer<BackupCachedLink> {
    override suspend fun invoke(items: List<BackupCachedLink>): Result<Unit> {
        return runCatching {
            val userId = userSessionDataStore.currentUserId.filterNotNull().first()

            items.forEach { item ->
                val media = item.media.toDbMedia()
                val externalIds = item.media.externalIds.map { it.toDbMediaExternalId() }

                cachedMediaLinkDao.insertMedia(media)
                cachedMediaLinkDao.insertMediaFts(item.media.toDbMediaFts())
                cachedMediaLinkDao.insertMediaIds(externalIds)

                when (item) {
                    is BackupCachedStream -> cachedMediaLinkDao.insertStream(item.toDbStream(userId))
                    is BackupCachedSubtitle -> cachedMediaLinkDao.insertSubtitle(item.toDbSubtitle(userId))
                }
            }
        }
    }

    private fun BackupDbMedia.toDbMedia(): DBMedia {
        return DBMedia(
            id = id,
            title = title,
            providerId = providerId,
            adult = adult,
            type = mediaType,
            overview = overview,
            posterImage = posterImage,
            language = language,
            rating = rating,
            backdropImage = backdropImage,
            releaseDate = releaseDate,
            createdAt = Date(createdAt),
            updatedAt = Date(updatedAt),
        )
    }

    private fun BackupDbMedia.toDbMediaFts(): DBMediaFts {
        return DBMediaFts(
            mediaId = id,
            title = title,
            overview = overview ?: "",
        )
    }

    private fun BackupDbMediaExternalId.toDbMediaExternalId(): DBMediaExternalId {
        return DBMediaExternalId(
            mediaId = mediaId,
            providerId = providerId,
            source = source,
            externalId = externalId,
            createdAt = Date(createdAt),
            updatedAt = Date(updatedAt),
        )
    }

    private fun BackupCachedStream.toDbStream(ownerId: String): CachedStream {
        return CachedStream(
            url = url,
            label = label,
            description = description,
            customHeaders = customHeaders,
            isDead = isDead,
            providerId = providerId,
            ownerId = ownerId,
            mediaId = mediaId,
            episodeNumber = episodeNumber,
            seasonNumber = seasonNumber,
            createdAt = Date(createdAt),
            updatedAt = Date(updatedAt),
            expiresOn = expiresOn,
            isThirdPartyGateway = isThirdPartyGateway,
            thirdPartyGatewayName = thirdPartyGatewayName,
            thirdPartyGatewayLogo = thirdPartyGatewayLogo,
        )
    }

    private fun BackupCachedSubtitle.toDbSubtitle(ownerId: String): CachedSubtitle {
        return CachedSubtitle(
            url = url,
            label = label,
            description = description,
            customHeaders = customHeaders,
            isDead = isDead,
            providerId = providerId,
            ownerId = ownerId,
            mediaId = mediaId,
            episodeNumber = episodeNumber,
            seasonNumber = seasonNumber,
            createdAt = Date(createdAt),
            updatedAt = Date(updatedAt),
            subtitleSource = subtitleSource,
        )
    }
}
