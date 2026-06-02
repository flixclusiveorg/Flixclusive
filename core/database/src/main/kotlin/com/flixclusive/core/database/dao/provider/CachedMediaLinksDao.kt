package com.flixclusive.core.database.dao.provider

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.flixclusive.core.database.entity.media.DBMedia
import com.flixclusive.core.database.entity.provider.CachedMediaLinks
import com.flixclusive.core.database.entity.provider.CachedMediaLinksWithData
import kotlinx.coroutines.flow.Flow

@Dao
interface CachedMediaLinksDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entry: CachedMediaLinks): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMedia(media: DBMedia): Long

    @Transaction
    @Query(
        """
        SELECT * FROM cached_media_links
        WHERE ownerId       = :ownerId
          AND mediaId       = :mediaId
          AND episodeNumber IS :episodeNumber
          AND seasonNumber  IS :seasonNumber
        LIMIT 1
        """,
    )
    suspend fun getByKey(
        ownerId: String,
        mediaId: String,
        episodeNumber: Int?,
        seasonNumber: Int?,
    ): CachedMediaLinksWithData?

    @Transaction
    @Query(
        """
        SELECT * FROM cached_media_links
        WHERE ownerId       = :ownerId
          AND mediaId       = :mediaId
          AND episodeNumber IS :episodeNumber
          AND seasonNumber  IS :seasonNumber
        LIMIT 1
        """,
    )
    fun getByKeyAsFlow(
        ownerId: String,
        mediaId: String,
        episodeNumber: Int?,
        seasonNumber: Int?,
    ): Flow<CachedMediaLinksWithData?>

    @Transaction
    @Query("SELECT * FROM cached_media_links WHERE id = :id")
    suspend fun getById(id: String): CachedMediaLinksWithData?

    @Transaction
    @Query("SELECT * FROM cached_media_links WHERE id = :id")
    fun getByIdAsFlow(id: String): Flow<CachedMediaLinksWithData?>

    @Transaction
    @Query("SELECT * FROM cached_media_links WHERE ownerId = :ownerId")
    fun getAllAsFlow(ownerId: String): Flow<List<CachedMediaLinksWithData>>

    @Query("""
        SELECT COUNT(*)
        FROM (
            SELECT parentId
            FROM cached_streams
            WHERE parentId IN (
                SELECT id
                FROM cached_media_links
                WHERE ownerId = :ownerId
            )

            UNION ALL

            SELECT parentId
            FROM cached_subtitles
            WHERE parentId IN (
                SELECT id
                FROM cached_media_links
                WHERE ownerId = :ownerId
            )
        )
    """)
    fun getCacheSize(ownerId: String): Flow<Int>

    @Transaction
    @Query("SELECT * FROM cached_media_links WHERE ownerId = :ownerId AND mediaId = :mediaId")
    fun getAllByMediaAsFlow(ownerId: String, mediaId: String): Flow<List<CachedMediaLinksWithData>>

    @Transaction
    suspend fun insertCache(cache: CachedMediaLinks, media: DBMedia? = null) {
        if (media != null) {
            insertMedia(media)
        }

        insert(cache)
    }

    @Query("DELETE FROM cached_media_links WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM cached_media_links WHERE ownerId = :ownerId")
    suspend fun deleteAll(ownerId: String)

    @Query(
        """
        DELETE FROM cached_media_links
        WHERE ownerId = :ownerId
          AND NOT EXISTS (
              SELECT 1 FROM cached_streams    WHERE parentId = cached_media_links.id
          )
          AND NOT EXISTS (
              SELECT 1 FROM cached_subtitles WHERE parentId = cached_media_links.id
          )
        """,
    )
    suspend fun deleteOrphaned(ownerId: String)
}
