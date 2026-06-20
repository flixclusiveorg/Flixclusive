package com.flixclusive.core.database.dao.provider

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.flixclusive.core.database.entity.provider.DBStream
import com.flixclusive.core.database.entity.provider.DBSubtitle

@Dao
interface DBMediaLinkDao {
    @Upsert
    suspend fun upsertStream(stream: DBStream)

    @Upsert
    suspend fun upsertSubtitle(subtitle: DBSubtitle)

    @Query("UPDATE cached_streams SET isDead = 1, updatedAt = :now WHERE url = :url AND parentId = :parentId")
    suspend fun markStreamAsDead(url: String, parentId: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE cached_streams SET isDead = 0, updatedAt = :now WHERE url = :url AND parentId = :parentId")
    suspend fun markStreamAsAlive(url: String, parentId: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE cached_subtitles SET isDead = 1, updatedAt = :now WHERE url = :url AND parentId = :parentId")
    suspend fun markSubtitleAsDead(url: String, parentId: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE cached_subtitles SET isDead = 0, updatedAt = :now WHERE url = :url AND parentId = :parentId")
    suspend fun markSubtitleAsAlive(url: String, parentId: String, now: Long = System.currentTimeMillis())

    @Transaction
    suspend fun markLinkAsAlive(url: String, parentId: String) {
        markStreamAsAlive(url, parentId)
        markSubtitleAsAlive(url, parentId)
    }

    @Transaction
    suspend fun markLinkAsDead(url: String, parentId: String) {
        markStreamAsDead(url, parentId)
        markSubtitleAsDead(url, parentId)
    }

    @Query("DELETE FROM cached_streams WHERE url = :url AND parentId = :parentId")
    suspend fun deleteStream(url: String, parentId: String)

    @Query("DELETE FROM cached_subtitles WHERE url = :url AND parentId = :parentId")
    suspend fun deleteSubtitle(url: String, parentId: String)

    @Transaction
    suspend fun deleteLink(url: String, parentId: String) {
        deleteStream(url, parentId)
        deleteSubtitle(url, parentId)
    }

    @Query("DELETE FROM cached_streams WHERE isDead = 1 AND parentId = :parentId")
    suspend fun deleteDeadStreams(parentId: String)

    @Query("DELETE FROM cached_subtitles WHERE isDead = 1 AND parentId = :parentId")
    suspend fun deleteDeadSubtitles(parentId: String)

    @Query(
        """
        DELETE FROM cached_streams
        WHERE isDead = 1
        AND updatedAt < :cutoffTimestamp
        AND parentId IN (
            SELECT id FROM cached_media_links WHERE ownerId = :ownerId
        )
        """
    )
    suspend fun deleteExpiredDeadStreams(ownerId: String, cutoffTimestamp: Long)

    @Query(
        """
        DELETE FROM cached_subtitles
        WHERE isDead = 1
        AND updatedAt < :cutoffTimestamp
        AND parentId IN (
            SELECT id FROM cached_media_links WHERE ownerId = :ownerId
        )
        """
    )
    suspend fun deleteExpiredDeadSubtitles(ownerId: String, cutoffTimestamp: Long)

    @Transaction
    suspend fun deleteExpiredDeadLinks(ownerId: String, cutoffTimestamp: Long) {
        deleteExpiredDeadStreams(ownerId, cutoffTimestamp)
        deleteExpiredDeadSubtitles(ownerId, cutoffTimestamp)
    }
}
