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

    /**
     * Inserts [item], or returns the id of the row already downloading the same media when one
     * exists. Uniqueness is enforced by the database rather than by a read-then-insert check, so two
     * taps racing each other can't both slip through.
     *
     * @return the id that ended up representing this download — [DownloadItem.id] on a fresh insert,
     * the incumbent's id otherwise.
     */
    suspend fun queue(item: DownloadItem): String

    suspend fun getOldestQueuedItem(): DownloadItem?

    /**
     * The finished download for exactly this media/episode, or null when there isn't one.
     *
     * Keyed by [com.flixclusive.core.database.entity.downloads.dedupeKeyOf] rather than by a
     * three-column match, because that key is what the unique index is built on — one row at
     * most can ever answer it.
     */
    suspend fun getCompletedFor(
        mediaId: String,
        seasonNumber: Int?,
        episodeNumber: Int?,
    ): DownloadItem?

    suspend fun getBatch(
        mediaId: String,
        seasonNumber: Int,
    ): List<DownloadItem>

    fun observeBatch(
        mediaId: String,
        seasonNumber: Int,
    ): Flow<List<DownloadItem>>

    /** Every downloaded item for a media, across all seasons — used to synthesize an offline
     * season/episode list for local playback. */
    fun observeByMedia(mediaId: String): Flow<List<DownloadItem>>

    suspend fun updateState(
        id: String,
        state: DownloadItemState,
        phase: DownloadPhase?,
    )

    /**
     * Requeues every item left mid-transfer by a process death — [DownloadItemState.DOWNLOADING_STREAM],
     * [DownloadItemState.FETCHING_SUBTITLES] and [DownloadItemState.STREAM_COMPLETE] — back to
     * [DownloadItemState.QUEUED] with the phase they should resume into, so the dispatcher picks
     * them up again. [DownloadItemState.PAUSED] is deliberately left alone: that one was the user's
     * choice. Items in [excludedIds] are skipped, for when a live transfer is already driving them.
     *
     * @return how many rows were requeued.
     */
    suspend fun requeueInterruptedItems(excludedIds: List<String>): Int

    suspend fun markError(
        id: String,
        message: String,
    )

    /** Clears [id]'s chunk bookkeeping and resets [DownloadItem.streamBytesDownloaded]/
     * [DownloadItem.streamTotalBytes]/[DownloadItem.downloadBytesPerSecond] to `0` — for when the
     * item is about to restart a transfer from scratch and the displayed progress should reset
     * too (retry, stop, a dead-link re-resolve). Use [deleteChunks] instead when clearing chunk
     * rows between transfers that shouldn't disturb what's already been reported (e.g. moving
     * from the video into the subtitle phase, where the video's final size should keep showing).
     */
    suspend fun resetChunks(id: String)

    /** Deletes [id]'s [com.flixclusive.core.database.entity.downloads.DownloadChunk] rows only —
     * unlike [resetChunks], leaves [DownloadItem.streamBytesDownloaded]/[DownloadItem.streamTotalBytes]/
     * [DownloadItem.downloadBytesPerSecond] untouched. */
    suspend fun deleteChunks(id: String)

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
