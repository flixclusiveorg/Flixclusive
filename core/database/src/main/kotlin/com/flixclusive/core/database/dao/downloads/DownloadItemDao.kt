package com.flixclusive.core.database.dao.downloads

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.flixclusive.core.database.entity.downloads.DownloadItem
import com.flixclusive.core.database.entity.downloads.DownloadItemState
import com.flixclusive.core.database.entity.downloads.DownloadPhase
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface DownloadItemDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(item: DownloadItem)

    @Update
    suspend fun update(item: DownloadItem)

    @Query("SELECT * FROM download_items WHERE id = :id")
    suspend fun get(id: String): DownloadItem?

    @Query("SELECT * FROM download_items WHERE id = :id")
    fun getAsFlow(id: String): Flow<DownloadItem?>

    @Query("SELECT * FROM download_items ORDER BY createdAt DESC")
    fun getAllAsFlow(): Flow<List<DownloadItem>>

    @Query("SELECT * FROM download_items WHERE state = :state ORDER BY createdAt ASC")
    suspend fun getAllByState(state: DownloadItemState): List<DownloadItem>

    @Query("SELECT * FROM download_items WHERE state = :state ORDER BY createdAt ASC LIMIT 1")
    suspend fun getOldestByState(state: DownloadItemState): DownloadItem?

    @Query(
        "SELECT * FROM download_items WHERE mediaId = :mediaId AND seasonNumber = :seasonNumber ORDER BY episodeNumber ASC",
    )
    suspend fun getBatch(
        mediaId: String,
        seasonNumber: Int,
    ): List<DownloadItem>

    @Query(
        "SELECT * FROM download_items WHERE mediaId = :mediaId AND seasonNumber = :seasonNumber ORDER BY episodeNumber ASC",
    )
    fun getBatchAsFlow(
        mediaId: String,
        seasonNumber: Int,
    ): Flow<List<DownloadItem>>

    /** Every downloaded item for a media — across all seasons, for a show — used to synthesize
     * an offline season/episode list for local playback. */
    @Query("SELECT * FROM download_items WHERE mediaId = :mediaId ORDER BY seasonNumber ASC, episodeNumber ASC")
    fun getByMediaAsFlow(mediaId: String): Flow<List<DownloadItem>>

    @Query(
        """
        UPDATE download_items
        SET state = :state, phase = :phase, updatedAt = :updatedAt
        WHERE id = :id
        """,
    )
    suspend fun updateState(
        id: String,
        state: DownloadItemState,
        phase: DownloadPhase?,
        updatedAt: Date,
    )

    /**
     * Bulk-requeues every row still sitting in one of [from] — the states a transfer can only be
     * left in by the process dying mid-download, since nothing else exits without writing a
     * terminal or paused state. [phase] is written explicitly rather than preserved so a requeued
     * row resumes into the right phase, and [excludedIds] shields any item a live coroutine is
     * still driving (the service can be recreated while downloads are running).
     */
    @Query(
        """
        UPDATE download_items
        SET state = :to, phase = :phase, downloadBytesPerSecond = 0, updatedAt = :updatedAt
        WHERE state IN (:from) AND id NOT IN (:excludedIds)
        """,
    )
    suspend fun requeueByStates(
        from: List<DownloadItemState>,
        to: DownloadItemState,
        phase: DownloadPhase?,
        excludedIds: List<String>,
        updatedAt: Date,
    ): Int

    @Query(
        """
        UPDATE download_items
        SET streamBytesDownloaded = :bytesDownloaded, streamTotalBytes = :totalBytes,
            downloadBytesPerSecond = :bytesPerSecond, updatedAt = :updatedAt
        WHERE id = :id
        """,
    )
    suspend fun updateStreamProgress(
        id: String,
        bytesDownloaded: Long,
        totalBytes: Long,
        bytesPerSecond: Long,
        updatedAt: Date,
    )

    /** Narrower sibling of [updateStreamProgress] for a subtitle transfer's rate — subtitles
     * report completion as a count ([downloadedSubtitlesCount]/[totalSubtitlesCount]), not
     * bytes, so this leaves [DownloadItem.streamBytesDownloaded]/[DownloadItem.streamTotalBytes]
     * alone and only updates [DownloadItem.downloadBytesPerSecond]. */
    @Query(
        """
        UPDATE download_items
        SET downloadBytesPerSecond = :bytesPerSecond, updatedAt = :updatedAt
        WHERE id = :id
        """,
    )
    suspend fun updateDownloadRate(
        id: String,
        bytesPerSecond: Long,
        updatedAt: Date,
    )

    @Query(
        """
        UPDATE download_items
        SET sourceUrl = :sourceUrl, isHlsStream = :isHlsStream,
            streamBytesDownloaded = 0, streamTotalBytes = :totalBytes, updatedAt = :updatedAt
        WHERE id = :id
        """,
    )
    suspend fun updateSource(
        id: String,
        sourceUrl: String?,
        isHlsStream: Boolean,
        totalBytes: Long,
        updatedAt: Date,
    )

    @Query("UPDATE download_items SET streamFilePath = :streamFilePath, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStreamFilePath(
        id: String,
        streamFilePath: String?,
        updatedAt: Date,
    )

    @Query("UPDATE download_items SET totalSubtitlesCount = :count, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setTotalSubtitlesCount(
        id: String,
        count: Int,
        updatedAt: Date,
    )

    @Query(
        """
        UPDATE download_items
        SET downloadedSubtitlesCount = downloadedSubtitlesCount + 1, updatedAt = :updatedAt
        WHERE id = :id
        """,
    )
    suspend fun incrementDownloadedSubtitlesCount(
        id: String,
        updatedAt: Date,
    )

    @Query("UPDATE download_items SET errorMessage = :errorMessage, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateError(
        id: String,
        errorMessage: String?,
        updatedAt: Date,
    )

    @Query("DELETE FROM download_items WHERE id = :id")
    suspend fun delete(id: String)
}
