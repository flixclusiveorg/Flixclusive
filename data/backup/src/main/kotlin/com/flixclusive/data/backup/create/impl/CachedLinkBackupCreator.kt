package com.flixclusive.data.backup.create.impl

import com.flixclusive.core.database.dao.provider.CachedMediaLinkDao
import com.flixclusive.core.database.entity.media.DBMedia
import com.flixclusive.core.database.entity.media.DBMediaExternalId
import com.flixclusive.core.database.entity.provider.CachedStream
import com.flixclusive.core.database.entity.provider.CachedSubtitle
import com.flixclusive.core.database.entity.provider.MediaLinksWithData
import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.data.backup.create.BackupCreator
import com.flixclusive.data.backup.model.BackupCachedLink
import com.flixclusive.data.backup.model.BackupCachedStream
import com.flixclusive.data.backup.model.BackupCachedSubtitle
import com.flixclusive.data.backup.model.BackupDbMedia
import com.flixclusive.data.backup.model.BackupDbMediaExternalId
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import javax.inject.Inject

internal class CachedLinkBackupCreator @Inject constructor(
    private val cachedMediaLinkDao: CachedMediaLinkDao,
    private val userSessionDataStore: UserSessionDataStore
) : BackupCreator<BackupCachedLink> {
    override suspend fun invoke(): Result<List<BackupCachedLink>> {
        return runCatching {
            val userId = userSessionDataStore.currentUserId.filterNotNull().first()

            val allLinksWithData = cachedMediaLinkDao.getAllMediaLinks(userId)

            allLinksWithData.flatMap { it.toBackupItems() }
        }
    }

    private fun MediaLinksWithData.toBackupItems(): List<BackupCachedLink> {
        val backupMedia = media.toBackupMedia(externalIds)

        val backupStreams = streams.map { it.toBackupItem(backupMedia) }
        val backupSubtitles = subtitles.map { it.toBackupItem(backupMedia) }

        return backupStreams + backupSubtitles
    }

    private fun DBMedia.toBackupMedia(externalIds: List<DBMediaExternalId>): BackupDbMedia {
        return BackupDbMedia(
            id = id,
            title = title,
            providerId = providerId,
            adult = adult,
            mediaType = type,
            overview = overview,
            posterImage = posterImage,
            language = language,
            rating = rating,
            backdropImage = backdropImage,
            releaseDate = releaseDate,
            createdAt = createdAt.time,
            updatedAt = updatedAt.time,
            externalIds = externalIds.map { it.toBackupExternalId() }
        )
    }

    private fun DBMediaExternalId.toBackupExternalId(): BackupDbMediaExternalId {
        return BackupDbMediaExternalId(
            mediaId = mediaId,
            providerId = providerId,
            source = source,
            externalId = externalId,
            createdAt = createdAt.time,
            updatedAt = updatedAt.time
        )
    }

    private fun CachedStream.toBackupItem(backupMedia: BackupDbMedia): BackupCachedStream {
        return BackupCachedStream(
            url = url,
            label = label,
            description = description,
            customHeaders = customHeaders,
            isDead = isDead,
            providerId = providerId,
            mediaId = mediaId,
            episodeNumber = episodeNumber,
            seasonNumber = seasonNumber,
            createdAt = createdAt.time,
            updatedAt = updatedAt.time,
            expiresOn = expiresOn,
            isThirdPartyGateway = isThirdPartyGateway,
            thirdPartyGatewayName = thirdPartyGatewayName,
            thirdPartyGatewayLogo = thirdPartyGatewayLogo,
            media = backupMedia
        )
    }

    private fun CachedSubtitle.toBackupItem(backupMedia: BackupDbMedia): BackupCachedSubtitle {
        return BackupCachedSubtitle(
            url = url,
            label = label,
            description = description,
            customHeaders = customHeaders,
            isDead = isDead,
            providerId = providerId,
            mediaId = mediaId,
            episodeNumber = episodeNumber,
            seasonNumber = seasonNumber,
            createdAt = createdAt.time,
            updatedAt = updatedAt.time,
            subtitleSource = subtitleSource,
            media = backupMedia
        )
    }
}
