package com.flixclusive.data.downloads.repository

import com.flixclusive.core.database.entity.downloads.DownloadItem
import com.flixclusive.core.database.entity.downloads.DownloadItemState
import com.flixclusive.core.database.entity.downloads.DownloadPhase
import com.flixclusive.data.downloads.hls.HlsSegmentInfo
import com.flixclusive.data.downloads.model.DownloadInterruptReason
import com.flixclusive.data.downloads.transfer.MediaTransferResult
import com.hippo.unifile.UniFile
import kotlinx.coroutines.flow.Flow

interface MediaDownloadRepository {
    fun observeItem(id: String): Flow<DownloadItem?>

    fun observeAllItems(): Flow<List<DownloadItem>>

    suspend fun getItem(id: String): DownloadItem?

    suspend fun queue(item: DownloadItem)

    suspend fun getOldestQueuedItem(): DownloadItem?

    suspend fun getBatch(
        mediaId: String,
        seasonNumber: Int,
    ): List<DownloadItem>

    fun observeBatch(
        mediaId: String,
        seasonNumber: Int,
    ): Flow<List<DownloadItem>>

    suspend fun updateState(
        id: String,
        state: DownloadItemState,
        phase: DownloadPhase?,
    )

    suspend fun markError(
        id: String,
        message: String,
    )

    suspend fun resetChunks(id: String)

    /**
     * Persists [sourceUrl]/[isHls] together and resets stream progress to `0`/[totalBytes] — the
     * single write path keeping [DownloadItem.sourceUrl] and [DownloadItem.isHlsStream] in
     * lockstep, so no caller can update one without the other. [totalBytes] should be the probed
     * content length for a direct file, or `0` for HLS (whose total is tracked as a segment count
     * once the manifest resolves) or when the length is unknown.
     */
    suspend fun updateSource(
        id: String,
        sourceUrl: String?,
        isHls: Boolean,
        totalBytes: Long,
    )

    suspend fun updateStreamFilePath(
        id: String,
        streamFilePath: String?,
    )

    suspend fun setTotalSubtitlesCount(
        id: String,
        count: Int,
    )

    suspend fun incrementDownloadedSubtitlesCount(id: String)

    suspend fun runTransfer(
        id: String,
        phase: DownloadPhase,
        url: String,
        headers: Map<String, String>,
        destinationFile: UniFile,
        totalBytes: Long?,
    ): MediaTransferResult

    /**
     * Downloads an HLS stream's segments, resuming from [startIndex]. Progress is written to the
     * same [DownloadItem.streamBytesDownloaded]/[DownloadItem.streamTotalBytes] columns as
     * [runTransfer], but as segment counts rather than byte counts — segments have no known byte
     * length up front, unlike byte-range chunks.
     */
    suspend fun runHlsTransfer(
        id: String,
        segments: List<HlsSegmentInfo>,
        startIndex: Int,
        headers: Map<String, String>,
        destinationFile: UniFile,
    ): MediaTransferResult

    fun requestInterrupt(
        id: String,
        reason: DownloadInterruptReason,
    )

    fun consumeInterruptReason(id: String): DownloadInterruptReason?

    suspend fun delete(id: String)
}
