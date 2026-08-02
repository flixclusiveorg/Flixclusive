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
    suspend fun insert(item: DownloadItem): Long

    @Update
    suspend fun update(item: DownloadItem)

    @Query("SELECT * FROM download_items WHERE id = :id")
    suspend fun get(id: Long): DownloadItem?

    @Query("SELECT * FROM download_items WHERE id = :id")
    fun getAsFlow(id: Long): Flow<DownloadItem?>

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

    @Query(
        """
        UPDATE download_items
        SET state = :state, phase = :phase, updatedAt = :updatedAt
        WHERE id = :id
        """,
    )
    suspend fun updateState(
        id: Long,
        state: DownloadItemState,
        phase: DownloadPhase?,
        updatedAt: Date,
    )

    @Query(
        """
        UPDATE download_items
        SET streamBytesDownloaded = :bytesDownloaded, streamTotalBytes = :totalBytes, updatedAt = :updatedAt
        WHERE id = :id
        """,
    )
    suspend fun updateStreamProgress(
        id: Long,
        bytesDownloaded: Long,
        totalBytes: Long,
        updatedAt: Date,
    )

    @Query(
        """
        UPDATE download_items
        SET subtitleBytesDownloaded = :bytesDownloaded, subtitleTotalBytes = :totalBytes, updatedAt = :updatedAt
        WHERE id = :id
        """,
    )
    suspend fun updateSubtitleProgress(
        id: Long,
        bytesDownloaded: Long,
        totalBytes: Long,
        updatedAt: Date,
    )

    @Query("UPDATE download_items SET streamUrl = :streamUrl, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStreamUrl(
        id: Long,
        streamUrl: String,
        updatedAt: Date,
    )

    @Query("UPDATE download_items SET errorMessage = :errorMessage, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateError(
        id: Long,
        errorMessage: String?,
        updatedAt: Date,
    )

    @Query("UPDATE download_items SET subtitleError = :subtitleError, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateSubtitleError(
        id: Long,
        subtitleError: String?,
        updatedAt: Date,
    )

    @Query("DELETE FROM download_items WHERE id = :id")
    suspend fun delete(id: Long)
}
