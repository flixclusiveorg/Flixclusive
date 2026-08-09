package com.flixclusive.data.downloads.repository.impl

import android.database.sqlite.SQLiteConstraintException
import com.flixclusive.core.database.dao.downloads.DownloadChunkDao
import com.flixclusive.core.database.dao.downloads.DownloadItemDao
import com.flixclusive.core.database.entity.downloads.DownloadChunk
import com.flixclusive.core.database.entity.downloads.DownloadChunkStatus
import com.flixclusive.core.database.entity.downloads.DownloadItem
import com.flixclusive.core.database.entity.downloads.DownloadItemState
import com.flixclusive.core.database.entity.downloads.DownloadPhase
import com.flixclusive.core.database.entity.downloads.dedupeKeyOf
import com.flixclusive.data.downloads.hls.HlsSegmentInfo
import com.flixclusive.data.downloads.hls.HlsTransferEngine
import com.flixclusive.data.downloads.model.DownloadInterruptReason
import com.flixclusive.data.downloads.repository.MediaDownloadRepository
import com.flixclusive.data.downloads.transfer.MediaTransferEngine
import com.flixclusive.data.downloads.transfer.MediaTransferResult
import com.flixclusive.data.downloads.transfer.RangeUnsupportedException
import com.flixclusive.data.downloads.util.ChunkPlanner
import com.hippo.unifile.UniFile
import kotlinx.coroutines.flow.Flow
import java.util.Date
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class MediaDownloadRepositoryImpl @Inject constructor(
    private val downloadItemDao: DownloadItemDao,
    private val downloadChunkDao: DownloadChunkDao,
    private val mediaTransferEngine: MediaTransferEngine,
    private val hlsTransferEngine: HlsTransferEngine,
) : MediaDownloadRepository {
    private val interruptFlags = ConcurrentHashMap<String, DownloadInterruptReason>()

    private val tracker = TransferProgressTracker()

    override fun observeAllItems(): Flow<List<DownloadItem>> = downloadItemDao.getAllAsFlow()

    override suspend fun getItem(id: String): DownloadItem? = downloadItemDao.get(id)

    override suspend fun queue(item: DownloadItem): String {
        // Recomputed rather than trusted: dedupeKey is a constructor default, so an item built with
        // copy() carries whichever key the original had. Deriving it here means the column can never
        // disagree with the media it is supposed to identify, however the caller assembled the row.
        val row = item.copy(dedupeKey = dedupeKeyOf(item.mediaId, item.seasonNumber, item.episodeNumber))

        return try {
            downloadItemDao.insert(row)
            row.id
        } catch (_: SQLiteConstraintException) {
            // The unique dedupeKey index rejected it: something is already downloading this exact
            // media. Hand back the incumbent so the caller carries on with that one instead of
            // treating a harmless double tap as an error.
            downloadItemDao.getByDedupeKey(row.dedupeKey)?.id ?: row.id
        }
    }

    override suspend fun getOldestQueuedItem(): DownloadItem? = downloadItemDao.getOldestByState(
        DownloadItemState.QUEUED
    )

    override suspend fun getFor(
        mediaId: String,
        seasonNumber: Int?,
        episodeNumber: Int?,
    ): DownloadItem? = downloadItemDao.getByDedupeKey(dedupeKeyOf(mediaId, seasonNumber, episodeNumber))

    override suspend fun getCompletedFor(
        mediaId: String,
        seasonNumber: Int?,
        episodeNumber: Int?,
    ): DownloadItem? = getFor(mediaId, seasonNumber, episodeNumber)
        ?.takeIf { it.state == DownloadItemState.COMPLETED }

    override suspend fun getBatch(
        mediaId: String,
        seasonNumber: Int,
    ): List<DownloadItem> = downloadItemDao.getBatch(mediaId, seasonNumber)

    override fun observeBatch(
        mediaId: String,
        seasonNumber: Int,
    ): Flow<List<DownloadItem>> = downloadItemDao.getBatchAsFlow(mediaId, seasonNumber)

    override fun observeByMedia(mediaId: String): Flow<List<DownloadItem>> = downloadItemDao.getByMediaAsFlow(mediaId)

    override suspend fun updateState(
        id: String,
        state: DownloadItemState,
        phase: DownloadPhase?,
    ) {
        downloadItemDao.updateState(id, state, phase, Date())
    }

    override suspend fun markError(
        id: String,
        message: String,
    ) {
        downloadItemDao.updateError(id, message, Date())
    }

    override suspend fun resetChunks(id: String) {
        deleteChunks(id)
        // Unlike deleteChunks (which also runs between two subtitle files — exactly the gap the
        // hold-over exists to ride out), a reset restarts the transfer from nothing, so the speed
        // it was last running at is no longer worth replaying.
        tracker.clearRateHold(id)
        // Also zeroes the segment-count progress HLS items keep in the same columns, so a manual
        // retry restarts an HLS download instead of silently resuming it (matching the byte-range
        // engine, where deleting chunks already forces a from-scratch replan).
        downloadItemDao.updateStreamProgress(id, bytesDownloaded = 0, totalBytes = 0, bytesPerSecond = 0, Date())
    }

    override suspend fun deleteChunks(id: String) {
        downloadChunkDao.deleteChunksForItem(id)
        // Chunks are gone, so any in-flight rate sample no longer has a valid baseline to diff
        // against — without this, the first write after a resumed/restarted transfer would diff
        // against bytes/timing from a since-discarded run and read as a huge stale-timed sample.
        tracker.clearRateBaseline(id)
    }

    override suspend fun updateSource(
        id: String,
        sourceUrl: String?,
        isHls: Boolean,
        totalBytes: Long,
    ) {
        downloadItemDao.updateSource(id, sourceUrl, isHls, totalBytes, Date())
    }

    override suspend fun updateStreamFilePath(
        id: String,
        streamFilePath: String?,
    ) {
        downloadItemDao.updateStreamFilePath(id, streamFilePath, Date())
    }

    override suspend fun setTotalSubtitlesCount(
        id: String,
        count: Int,
    ) {
        downloadItemDao.setTotalSubtitlesCount(id, count, Date())
    }

    override suspend fun incrementDownloadedSubtitlesCount(id: String) {
        downloadItemDao.incrementDownloadedSubtitlesCount(id, Date())
    }

    override suspend fun runTransfer(
        id: String,
        phase: DownloadPhase,
        url: String,
        headers: Map<String, String>,
        destinationFile: UniFile,
        totalBytes: Long?,
    ): MediaTransferResult {
        // Honour, don't clear: an interrupt requested during a window between transfers — link
        // resolution, HLS manifest fetching, the hop from one subtitle file to the next — used to
        // be wiped here before anything ever polled it, which is how a stop could go missing
        // entirely. Leaving the flag set lets the caller's handleInterrupted consume it.
        if (interruptFlags.containsKey(id)) return MediaTransferResult.Cancelled

        val existingChunks = downloadChunkDao.getChunksForItem(id)
        val chunks =
            existingChunks.ifEmpty {
                val planned =
                    ChunkPlanner.plan(totalBytes).mapIndexed { index, range ->
                        DownloadChunk(
                            downloadItemId = id,
                            chunkIndex = index,
                            rangeStart = range.first,
                            rangeEnd = range.last
                        )
                    }
                downloadChunkDao.insertAll(planned)
                downloadChunkDao.getChunksForItem(id)
            }

        val result = runChunkedTransfer(id, phase, url, headers, destinationFile, totalBytes, chunks)

        // The server ignored Range, so the multi-chunk plan can never work against this link. Re-plan
        // as one open-ended chunk — the same shape used when no content length is known — and try
        // once more before giving up. Doing it here rather than persisting a per-link "supports
        // ranges" flag keeps it self-healing and avoids a probe change: the probe measures
        // throughput by reading the body, which a `bytes=0-0` request would defeat.
        if (result is MediaTransferResult.Failed && result.cause is RangeUnsupportedException && chunks.size > 1) {
            downloadChunkDao.deleteChunksForItem(id)
            downloadChunkDao.insertAll(
                listOf(
                    DownloadChunk(
                        downloadItemId = id,
                        chunkIndex = 0,
                        rangeStart = 0,
                        rangeEnd = ChunkPlanner.OPEN_ENDED_RANGE_END,
                    ),
                ),
            )

            return runChunkedTransfer(
                id = id,
                phase = phase,
                url = url,
                headers = headers,
                destinationFile = destinationFile,
                totalBytes = totalBytes,
                chunks = downloadChunkDao.getChunksForItem(id),
            )
        }

        return result
    }

    private suspend fun runChunkedTransfer(
        id: String,
        phase: DownloadPhase,
        url: String,
        headers: Map<String, String>,
        destinationFile: UniFile,
        totalBytes: Long?,
        chunks: List<DownloadChunk>,
    ): MediaTransferResult {
        tracker.startStallWatch(id)

        val result = mediaTransferEngine.transfer(
            chunks = chunks,
            url = url,
            headers = headers,
            destinationFile = destinationFile,
            shouldInterrupt = { interruptFlags.containsKey(id) || tracker.hasStalled(id, phase) },
        ) { chunkId, bytesDownloaded, status ->
            tracker.recordMovement(id, chunkId, bytesDownloaded)
            downloadChunkDao.updateProgress(chunkId, bytesDownloaded, status)
            writeAggregatedProgressThrottled(id, phase, totalBytes, status)
        }

        return tracker.resolveStalled(id, result, interrupted = interruptFlags.containsKey(id))
    }

    override suspend fun runHlsTransfer(
        id: String,
        segments: List<HlsSegmentInfo>,
        startIndex: Int,
        headers: Map<String, String>,
        destinationFile: UniFile,
    ): MediaTransferResult {
        if (interruptFlags.containsKey(id)) return MediaTransferResult.Cancelled

        tracker.startStallWatch(id)

        val result = hlsTransferEngine.transfer(
            segments = segments,
            startIndex = startIndex,
            headers = headers,
            destinationFile = destinationFile,
            shouldInterrupt = { interruptFlags.containsKey(id) || tracker.hasStalled(id, DownloadPhase.STREAM) },
        ) { segmentsWritten, totalSegments ->
            // Every callback here is a segment landing, so it is movement by definition — no need
            // to diff against a previous value the way the byte-range path does.
            tracker.markMoved(id)
            writeHlsProgressThrottled(id, segmentsWritten, totalSegments)
        }

        return tracker.resolveStalled(id, result, interrupted = interruptFlags.containsKey(id))
    }

    private suspend fun writeHlsProgressThrottled(
        id: String,
        segmentsWritten: Int,
        totalSegments: Int,
    ) {
        val now = System.currentTimeMillis()
        val lastWrite = tracker.lastWriteAt(id)
        val isFinal = segmentsWritten >= totalSegments
        if (!isFinal && now - lastWrite < PROGRESS_WRITE_THROTTLE_MS) return

        val rate = tracker.rateFor(id, segmentsWritten.toLong(), now, lastWrite)
        tracker.recordWrite(id, now, segmentsWritten.toLong())
        downloadItemDao.updateStreamProgress(id, segmentsWritten.toLong(), totalSegments.toLong(), rate, Date())
    }

    /** Only [DownloadPhase.STREAM] transfers write to [DownloadItem.streamBytesDownloaded] —
     * subtitles track completion as a count ([DownloadItem.downloadedSubtitlesCount]), not bytes,
     * even though they reuse this same chunked engine for their own resumable transfer. Both
     * phases still write [DownloadItem.downloadBytesPerSecond]: whichever transfer is currently
     * active gets a live speed. */
    private suspend fun writeAggregatedProgressThrottled(
        id: String,
        phase: DownloadPhase,
        totalBytes: Long?,
        status: DownloadChunkStatus,
    ) {
        val now = System.currentTimeMillis()
        val lastWrite = tracker.lastWriteAt(id)
        val shouldWrite = status != DownloadChunkStatus.DOWNLOADING || now - lastWrite >= PROGRESS_WRITE_THROTTLE_MS
        if (!shouldWrite) return

        val totalDownloaded = downloadChunkDao.getChunksForItem(id).sumOf { it.bytesDownloaded }
        val rate = tracker.rateFor(id, totalDownloaded, now, lastWrite)
        tracker.recordWrite(id, now, totalDownloaded)

        if (phase == DownloadPhase.STREAM) {
            downloadItemDao.updateStreamProgress(id, totalDownloaded, totalBytes ?: 0, rate, Date())
        } else {
            downloadItemDao.updateDownloadRate(id, rate, Date())
        }
    }

    override suspend fun requeueInterruptedItems(excludedIds: List<String>): Int {
        val now = Date()
        val requeuedStreams = downloadItemDao.requeueByStates(
            from = listOf(DownloadItemState.DOWNLOADING_STREAM),
            to = DownloadItemState.QUEUED,
            phase = DownloadPhase.STREAM,
            excludedIds = excludedIds,
            updatedAt = now,
        )
        // STREAM_COMPLETE joins FETCHING_SUBTITLES here rather than resuming as STREAM: its video
        // is already fully written, so the only work left for either is the subtitle phase.
        val requeuedSubtitles = downloadItemDao.requeueByStates(
            from = listOf(DownloadItemState.FETCHING_SUBTITLES, DownloadItemState.STREAM_COMPLETE),
            to = DownloadItemState.QUEUED,
            phase = DownloadPhase.SUBTITLES,
            excludedIds = excludedIds,
            updatedAt = now,
        )

        return requeuedStreams + requeuedSubtitles
    }

    override fun requestInterrupt(
        id: String,
        reason: DownloadInterruptReason,
    ) {
        interruptFlags[id] = reason
    }

    override fun consumeInterruptReason(id: String): DownloadInterruptReason? = interruptFlags.remove(id)

    override suspend fun delete(id: String) {
        tracker.forget(id)
        downloadItemDao.delete(id)
    }

    companion object {
        private const val PROGRESS_WRITE_THROTTLE_MS = 1000L
    }
}
