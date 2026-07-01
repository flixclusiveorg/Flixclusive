package com.flixclusive.core.database.dao.provider

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.flixclusive.core.database.entity.media.DBMedia
import com.flixclusive.core.database.entity.media.DBMedia.Companion.toDBMedia
import com.flixclusive.core.database.entity.media.DBMediaExternalId
import com.flixclusive.core.database.entity.media.DBMediaExternalId.Companion.toDBMediaExternalIds
import com.flixclusive.core.database.entity.media.DBMediaFts
import com.flixclusive.core.database.entity.media.DBMediaFts.Companion.toDBMediaFts
import com.flixclusive.core.database.entity.provider.CachedStream
import com.flixclusive.core.database.entity.provider.CachedSubtitle
import com.flixclusive.core.database.entity.provider.EpisodeLinks
import com.flixclusive.core.database.entity.provider.MediaLinksWithData
import com.flixclusive.core.database.entity.provider.SeasonLinks
import com.flixclusive.model.media.MediaMetadata
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import java.util.Date

@Dao
interface CachedMediaLinkDao {
    @Upsert
    suspend fun insertStream(stream: CachedStream)

    @Upsert
    suspend fun insertSubtitle(subtitle: CachedSubtitle)

    @Upsert
    suspend fun insertMedia(media: DBMedia)

    @Upsert
    suspend fun insertMediaFts(mediaFts: DBMediaFts)

    @Upsert
    suspend fun insertMediaIds(externalIds: List<DBMediaExternalId>)

    @Transaction
    suspend fun upsertMedia(media: MediaMetadata) {
        val existingMedia = getMediaByIds(
            mediaId = media.id,
            providerId = media.providerId
        ) ?: media.toDBMedia()

        insertMedia(existingMedia.copy(updatedAt = Date()))
        insertMediaFts(media.toDBMediaFts())
        insertMediaIds(media.toDBMediaExternalIds())
    }

    @Query("UPDATE cached_streams SET isDead = :isDead, updatedAt = :now WHERE url = :url AND ownerId = :ownerId")
    suspend fun setStreamStatus(url: String, ownerId: String, isDead: Boolean, now: Long = System.currentTimeMillis())

    @Query("UPDATE cached_subtitles SET isDead = :isDead, updatedAt = :now WHERE url = :url AND ownerId = :ownerId")
    suspend fun setSubtitleStatus(url: String, ownerId: String, isDead: Boolean, now: Long = System.currentTimeMillis())

    @Transaction
    suspend fun setLinkStatus(url: String, ownerId: String, isDead: Boolean, now: Long = System.currentTimeMillis()) {
        setStreamStatus(url, ownerId, isDead, now)
        setSubtitleStatus(url, ownerId, isDead, now)
    }

    @Query("DELETE FROM cached_streams WHERE url = :url AND ownerId = :ownerId")
    suspend fun deleteStream(url: String, ownerId: String)

    @Query("DELETE FROM cached_subtitles WHERE url = :url AND ownerId = :ownerId")
    suspend fun deleteSubtitle(url: String, ownerId: String)

    @Transaction
    suspend fun deleteLink(url: String, ownerId: String) {
        deleteStream(url, ownerId)
        deleteSubtitle(url, ownerId)
    }

    @Query("DELETE FROM cached_streams WHERE ownerId = :ownerId AND mediaId = :mediaId")
    suspend fun deleteStreamsByMedia(ownerId: String, mediaId: String)

    @Query("DELETE FROM cached_subtitles WHERE ownerId = :ownerId AND mediaId = :mediaId")
    suspend fun deleteSubtitlesByMedia(ownerId: String, mediaId: String)

    @Transaction
    suspend fun deleteLinksByMedia(ownerId: String, mediaId: String) {
        deleteStreamsByMedia(ownerId, mediaId)
        deleteSubtitlesByMedia(ownerId, mediaId)
    }

    @Query(
        "DELETE FROM cached_streams WHERE ownerId = :ownerId AND mediaId = :mediaId AND seasonNumber = :seasonNumber"
    )
    suspend fun deleteStreamsBySeason(ownerId: String, mediaId: String, seasonNumber: Int)

    @Query(
        "DELETE FROM cached_subtitles WHERE ownerId = :ownerId AND mediaId = :mediaId AND seasonNumber = :seasonNumber"
    )
    suspend fun deleteSubtitlesBySeason(ownerId: String, mediaId: String, seasonNumber: Int)

    @Transaction
    suspend fun deleteLinksBySeason(ownerId: String, mediaId: String, seasonNumber: Int) {
        deleteStreamsBySeason(ownerId, mediaId, seasonNumber)
        deleteSubtitlesBySeason(ownerId, mediaId, seasonNumber)
    }

    @Query(
        "DELETE FROM cached_streams WHERE ownerId = :ownerId AND mediaId = :mediaId AND seasonNumber = :seasonNumber AND episodeNumber = :episodeNumber"
    )
    suspend fun deleteStreamsByEpisode(ownerId: String, mediaId: String, seasonNumber: Int, episodeNumber: Int)

    @Query(
        "DELETE FROM cached_subtitles WHERE ownerId = :ownerId AND mediaId = :mediaId AND seasonNumber = :seasonNumber AND episodeNumber = :episodeNumber"
    )
    suspend fun deleteSubtitlesByEpisode(ownerId: String, mediaId: String, seasonNumber: Int, episodeNumber: Int)

    @Transaction
    suspend fun deleteLinksByEpisode(ownerId: String, mediaId: String, seasonNumber: Int, episodeNumber: Int) {
        deleteStreamsByEpisode(ownerId, mediaId, seasonNumber, episodeNumber)
        deleteSubtitlesByEpisode(ownerId, mediaId, seasonNumber, episodeNumber)
    }

    @Query("DELETE FROM cached_streams WHERE ownerId = :ownerId")
    suspend fun deleteAllStreams(ownerId: String)

    @Query("DELETE FROM cached_subtitles WHERE ownerId = :ownerId")
    suspend fun deleteAllSubtitles(ownerId: String)

    @Transaction
    suspend fun deleteAll(ownerId: String) {
        deleteAllStreams(ownerId)
        deleteAllSubtitles(ownerId)
    }

    @Query("DELETE FROM cached_streams WHERE ownerId = :ownerId AND isDead = 1 AND updatedAt <= :cutoffTimestamp")
    suspend fun deleteExpiredDeadStreams(ownerId: String, cutoffTimestamp: Long)

    @Query("DELETE FROM cached_subtitles WHERE ownerId = :ownerId AND isDead = 1 AND updatedAt <= :cutoffTimestamp")
    suspend fun deleteExpiredDeadSubtitles(ownerId: String, cutoffTimestamp: Long)

    @Transaction
    suspend fun deleteExpiredDeadLinks(ownerId: String, cutoffTimestamp: Long) {
        deleteExpiredDeadStreams(ownerId, cutoffTimestamp)
        deleteExpiredDeadSubtitles(ownerId, cutoffTimestamp)
    }

    @Query(
        """
        DELETE FROM cached_streams
        WHERE ownerId = :ownerId
          AND isThirdPartyGateway = 1
          AND NOT EXISTS (
              SELECT 1 FROM cached_streams AS s2
              WHERE s2.ownerId = cached_streams.ownerId
                AND s2.providerId = cached_streams.providerId
                AND s2.mediaId = cached_streams.mediaId
                AND (s2.seasonNumber IS cached_streams.seasonNumber)
                AND (s2.episodeNumber IS cached_streams.episodeNumber)
                AND s2.isThirdPartyGateway = 0
          )
    """
    )
    suspend fun deleteThirdPartyOnlyStreams(ownerId: String)

    @Query(
        """
        DELETE FROM cached_subtitles
        WHERE ownerId = :ownerId
          AND EXISTS (
              SELECT 1 FROM cached_streams AS s
              WHERE s.ownerId = cached_subtitles.ownerId
                AND s.providerId = cached_subtitles.providerId
                AND s.mediaId = cached_subtitles.mediaId
                AND (s.seasonNumber IS cached_subtitles.seasonNumber)
                AND (s.episodeNumber IS cached_subtitles.episodeNumber)
                AND s.isThirdPartyGateway = 1
          )
          AND NOT EXISTS (
              SELECT 1 FROM cached_streams AS s2
              WHERE s2.ownerId = cached_subtitles.ownerId
                AND s2.providerId = cached_subtitles.providerId
                AND s2.mediaId = cached_subtitles.mediaId
                AND (s2.seasonNumber IS cached_subtitles.seasonNumber)
                AND (s2.episodeNumber IS cached_subtitles.episodeNumber)
                AND s2.isThirdPartyGateway = 0
          )
    """
    )
    suspend fun deleteSubtitlesWithOnlyThirdPartyStreams(ownerId: String)

    @Transaction
    suspend fun deleteThirdPartyOnlyLinks(ownerId: String) {
        deleteSubtitlesWithOnlyThirdPartyStreams(ownerId)
        deleteThirdPartyOnlyStreams(ownerId)
    }

    @Query(
        """
        SELECT COUNT(*)
        FROM (
            SELECT url FROM cached_streams WHERE ownerId = :ownerId
            UNION ALL
            SELECT url FROM cached_subtitles WHERE ownerId = :ownerId
        )
        """
    )
    fun getCacheSize(ownerId: String): Flow<Int>

    @Query(
        """
        SELECT seasonNumber AS number, COUNT(*) AS count
        FROM (
            SELECT seasonNumber FROM cached_streams WHERE mediaId = :mediaId AND ownerId = :ownerId
            UNION ALL
            SELECT seasonNumber FROM cached_subtitles WHERE mediaId = :mediaId AND ownerId = :ownerId
        )
        WHERE seasonNumber IS NOT NULL
        GROUP BY seasonNumber
        """
    )
    fun getCachedSeasons(mediaId: String, ownerId: String): Flow<List<SeasonLinks>>

    @Query(
        """
        SELECT episodeNumber AS number, COUNT(*) AS count, MAX(updatedAt) AS lastUpdated
        FROM (
            SELECT episodeNumber, updatedAt FROM cached_streams WHERE mediaId = :mediaId AND seasonNumber = :seasonNumber AND ownerId = :ownerId
            UNION ALL
            SELECT episodeNumber, updatedAt FROM cached_subtitles WHERE mediaId = :mediaId AND seasonNumber = :seasonNumber AND ownerId = :ownerId
        )
        WHERE episodeNumber IS NOT NULL
        GROUP BY episodeNumber
        """
    )
    fun getCachedEpisodes(mediaId: String, seasonNumber: Int, ownerId: String): Flow<List<EpisodeLinks>>

    @Query("SELECT * FROM media WHERE id = :mediaId")
    suspend fun getMediaById(mediaId: String): DBMedia?

    @Query("SELECT * FROM media WHERE id = :mediaId AND providerId = :providerId")
    suspend fun getMediaByIds(mediaId: String, providerId: String): DBMedia?

    @Query(
        """
        SELECT * FROM cached_streams
        WHERE mediaId = :mediaId AND ownerId = :ownerId
            AND (:providerId IS NULL OR providerId = :providerId)
            AND (:episodeNumber IS NULL OR episodeNumber = :episodeNumber)
            AND (:seasonNumber IS NULL OR seasonNumber = :seasonNumber)
        """,
    )
    suspend fun getStreams(
        mediaId: String,
        ownerId: String,
        providerId: String? = null,
        episodeNumber: Int? = null,
        seasonNumber: Int? = null,
    ): List<CachedStream>

    @Query(
        """
        SELECT * FROM cached_subtitles
        WHERE mediaId = :mediaId AND ownerId = :ownerId
            AND (:providerId IS NULL OR providerId = :providerId)
            AND (:episodeNumber IS NULL OR episodeNumber = :episodeNumber)
            AND (:seasonNumber IS NULL OR seasonNumber = :seasonNumber)
        """,
    )
    suspend fun getSubtitles(
        mediaId: String,
        ownerId: String,
        providerId: String? = null,
        episodeNumber: Int? = null,
        seasonNumber: Int? = null,
    ): List<CachedSubtitle>

    @Query("SELECT * FROM media_external_ids WHERE mediaId = :mediaId")
    suspend fun getExternalIdsByMediaId(mediaId: String): List<DBMediaExternalId>

    @Query("SELECT * FROM media_external_ids WHERE mediaId = :mediaId AND providerId = :providerId")
    suspend fun getExternalIdsByIds(mediaId: String, providerId: String): List<DBMediaExternalId>

    @Transaction
    suspend fun getLinks(
        ownerId: String,
        mediaId: String,
        episodeNumber: Int?,
        seasonNumber: Int?,
    ): List<MediaLinksWithData> {
        val media = getMediaById(mediaId) ?: return emptyList()
        val externalIds = getExternalIdsByMediaId(mediaId)
        val streams = getStreams(mediaId, ownerId, null, episodeNumber, seasonNumber)
        val subtitles = getSubtitles(mediaId, ownerId, null, episodeNumber, seasonNumber)

        return groupLinks(media, streams, subtitles, externalIds)
    }

    @Transaction
    suspend fun getLinksByProvider(
        ownerId: String,
        providerId: String,
        mediaId: String,
        episodeNumber: Int?,
        seasonNumber: Int?,
    ): MediaLinksWithData? {
        val media = getMediaByIds(mediaId, providerId) ?: return null
        val externalIds = getExternalIdsByIds(mediaId, providerId)
        val streams = getStreams(mediaId, ownerId, providerId, episodeNumber, seasonNumber)
        val subtitles = getSubtitles(mediaId, ownerId, providerId, episodeNumber, seasonNumber)

        if (streams.isEmpty() && subtitles.isEmpty()) return null

        return MediaLinksWithData(
            media = media,
            streams = streams,
            subtitles = subtitles,
            externalIds = externalIds,
        )
    }

    @Transaction
    @Query("SELECT * FROM media WHERE id = :mediaId")
    fun observeMediaById(mediaId: String): Flow<DBMedia?>

    @Query(
        """
        SELECT * FROM cached_streams
        WHERE mediaId = :mediaId AND ownerId = :ownerId
            AND (:providerId IS NULL OR providerId = :providerId)
            AND (:episodeNumber IS NULL OR episodeNumber = :episodeNumber)
            AND (:seasonNumber IS NULL OR seasonNumber = :seasonNumber)
        """,
    )
    fun observeStreams(
        mediaId: String,
        ownerId: String,
        providerId: String? = null,
        episodeNumber: Int? = null,
        seasonNumber: Int? = null,
    ): Flow<List<CachedStream>>

    @Query(
        """
        SELECT * FROM cached_subtitles
        WHERE mediaId = :mediaId AND ownerId = :ownerId
            AND (:providerId IS NULL OR providerId = :providerId)
            AND (:episodeNumber IS NULL OR episodeNumber = :episodeNumber)
            AND (:seasonNumber IS NULL OR seasonNumber = :seasonNumber)
        """,
    )
    fun observeSubtitles(
        mediaId: String,
        ownerId: String,
        providerId: String? = null,
        episodeNumber: Int? = null,
        seasonNumber: Int? = null,
    ): Flow<List<CachedSubtitle>>

    @Query("SELECT * FROM media_external_ids WHERE mediaId = :mediaId")
    fun observeExternalIdsByMediaId(mediaId: String): Flow<List<DBMediaExternalId>>

    fun observeLinks(
        ownerId: String,
        mediaId: String,
        episodeNumber: Int?,
        seasonNumber: Int?,
    ): Flow<List<MediaLinksWithData>> {
        return combine(
            observeMediaById(mediaId),
            observeStreams(mediaId, ownerId, null, episodeNumber, seasonNumber),
            observeSubtitles(mediaId, ownerId, null, episodeNumber, seasonNumber),
            observeExternalIdsByMediaId(mediaId),
        ) { media, streams, subtitles, externalIds ->
            if (media == null) return@combine emptyList()

            groupLinks(media, streams, subtitles, externalIds)
        }
    }

    fun observeLinksByProvider(
        ownerId: String,
        providerId: String,
        mediaId: String,
        episodeNumber: Int?,
        seasonNumber: Int?,
    ): Flow<MediaLinksWithData?> {
        return combine(
            observeMediaById(mediaId),
            observeStreams(mediaId, ownerId, providerId, episodeNumber, seasonNumber),
            observeSubtitles(mediaId, ownerId, providerId, episodeNumber, seasonNumber),
            observeExternalIdsByMediaId(mediaId),
        ) { media, streams, subtitles, externalIds ->
            if (media == null || (streams.isEmpty() && subtitles.isEmpty())) return@combine null

            MediaLinksWithData(
                media = media.copy(providerId = providerId),
                streams = streams,
                subtitles = subtitles,
                externalIds = externalIds,
            )
        }
    }

    @Transaction
    suspend fun getAllMediaLinks(ownerId: String): List<MediaLinksWithData> {
        val mediaIds = getAllMediaIdsWithLinks(ownerId)
        val medias = getMediasByIds(mediaIds).associateBy { it.id }
        val externalIds = getExternalIdsByMediaIds(mediaIds).groupBy { it.mediaId }
        val streams = getStreamsByOwner(ownerId).groupBy { it.mediaId }
        val subtitles = getSubtitlesByOwner(ownerId).groupBy { it.mediaId }

        return mediaIds.flatMap { mediaId ->
            val media = medias[mediaId] ?: return@flatMap emptyList()
            groupLinks(
                media = media,
                streams = streams[mediaId] ?: emptyList(),
                subtitles = subtitles[mediaId] ?: emptyList(),
                externalIds = externalIds[mediaId] ?: emptyList(),
            )
        }
    }

    @Query("SELECT * FROM media WHERE id IN (:mediaIds)")
    suspend fun getMediasByIds(mediaIds: List<String>): List<DBMedia>

    @Query("SELECT * FROM media_external_ids WHERE mediaId IN (:mediaIds)")
    suspend fun getExternalIdsByMediaIds(mediaIds: List<String>): List<DBMediaExternalId>

    @Query("SELECT * FROM cached_streams WHERE ownerId = :ownerId")
    suspend fun getStreamsByOwner(ownerId: String): List<CachedStream>

    @Query("SELECT * FROM cached_subtitles WHERE ownerId = :ownerId")
    suspend fun getSubtitlesByOwner(ownerId: String): List<CachedSubtitle>

    @Query(
        """
        SELECT DISTINCT mediaId FROM cached_streams WHERE ownerId = :ownerId
        UNION
        SELECT DISTINCT mediaId FROM cached_subtitles WHERE ownerId = :ownerId
        """,
    )
    suspend fun getAllMediaIdsWithLinks(ownerId: String): List<String>

    fun observeAllMediaLinks(ownerId: String): Flow<List<MediaLinksWithData>> {
        return observeAllMediaIdsWithLinks(ownerId).flatMapLatest { mediaIds ->
            if (mediaIds.isEmpty()) return@flatMapLatest flowOf(emptyList())

            combine(
                observeMediasByIds(mediaIds),
                observeExternalIdsByMediaIds(mediaIds),
                observeStreamsByOwner(ownerId),
                observeSubtitlesByOwner(ownerId),
            ) { medias, externalIds, streams, subtitles ->
                val mediasMap = medias.associateBy { it.id }
                val externalIdsMap = externalIds.groupBy { it.mediaId }
                val streamsMap = streams.groupBy { it.mediaId }
                val subtitlesMap = subtitles.groupBy { it.mediaId }

                mediaIds.flatMap { mediaId ->
                    val media = mediasMap[mediaId] ?: return@flatMap emptyList()
                    groupLinks(
                        media = media,
                        streams = streamsMap[mediaId] ?: emptyList(),
                        subtitles = subtitlesMap[mediaId] ?: emptyList(),
                        externalIds = externalIdsMap[mediaId] ?: emptyList(),
                    )
                }
            }
        }
    }

    @Query("SELECT * FROM media WHERE id IN (:mediaIds)")
    fun observeMediasByIds(mediaIds: List<String>): Flow<List<DBMedia>>

    @Query("SELECT * FROM media_external_ids WHERE mediaId IN (:mediaIds)")
    fun observeExternalIdsByMediaIds(mediaIds: List<String>): Flow<List<DBMediaExternalId>>

    @Query("SELECT * FROM cached_streams WHERE ownerId = :ownerId")
    fun observeStreamsByOwner(ownerId: String): Flow<List<CachedStream>>

    @Query("SELECT * FROM cached_subtitles WHERE ownerId = :ownerId")
    fun observeSubtitlesByOwner(ownerId: String): Flow<List<CachedSubtitle>>

    @Query(
        """
        SELECT DISTINCT mediaId FROM cached_streams WHERE ownerId = :ownerId
        UNION
        SELECT DISTINCT mediaId FROM cached_subtitles WHERE ownerId = :ownerId
        """,
    )
    fun observeAllMediaIdsWithLinks(ownerId: String): Flow<List<String>>

    private fun groupLinks(
        media: DBMedia,
        streams: List<CachedStream>,
        subtitles: List<CachedSubtitle>,
        externalIds: List<DBMediaExternalId>
    ): List<MediaLinksWithData> {
        val allLinks = streams + subtitles
        return allLinks.groupBy { it.providerId }.map { (providerId, links) ->
            MediaLinksWithData(
                media = media.copy(providerId = providerId),
                streams = links.filterIsInstance<CachedStream>(),
                subtitles = links.filterIsInstance<CachedSubtitle>(),
                externalIds = externalIds
            )
        }
    }
}
