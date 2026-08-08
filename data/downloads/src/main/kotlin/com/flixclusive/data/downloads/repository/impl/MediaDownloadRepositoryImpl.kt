package com.flixclusive.data.downloads.repository.impl

import com.flixclusive.core.database.dao.downloads.DownloadChunkDao
import com.flixclusive.core.database.dao.downloads.DownloadItemDao
import com.flixclusive.core.database.entity.downloads.DownloadChunk
import com.flixclusive.core.database.entity.downloads.DownloadChunkStatus
import com.flixclusive.core.database.entity.downloads.DownloadItem
import com.flixclusive.core.database.entity.downloads.DownloadItemState
import com.flixclusive.core.database.entity.downloads.DownloadPhase
import com.flixclusive.data.downloads.hls.HlsSegmentInfo
import com.flixclusive.data.downloads.hls.HlsTransferEngine
import com.flixclusive.data.downloads.model.DownloadInterruptReason
import com.flixclusive.data.downloads.repository.MediaDownloadRepository
import com.flixclusive.data.downloads.transfer.MediaTransferEngine
import com.flixclusive.data.downloads.transfer.MediaTransferResult
import com.flixclusive.data.downloads.util.ChunkPlanner
import com.hippo.unifile.UniFile
import kotlinx.coroutines.flow.Flow
import java.io.IOException
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
    private val lastProgressWriteTimes = ConcurrentHashMap<String, Long>()

    /** The bytes-(or segments-, for HLS)-downloaded value written at [lastProgressWriteTimes]'s
     * timestamp for the same id — the previous sample [computeRate] diffs against. */
    private val lastProgressValues = ConcurrentHashMap<String, Long>()

    /** Last rate [computeRate] actually measured above zero, per id — what [smoothedRate] replays
     * while a transfer briefly stalls. */
    private val lastNonZeroRates = ConcurrentHashMap<String, Long>()

    /** How many consecutive zero samples [smoothedRate] has already covered for with
     * [lastNonZeroRates], per id. */
    private val heldRateSampleCounts = ConcurrentHashMap<String, Int>()

    /** When each in-flight transfer last made real headway (see [recordMovement]). OkHttp's own
     * read timeout only catches a socket that has gone completely silent; a connection that
     * dribbles out just enough data to keep resetting it would otherwise sit at ~0 B/s forever. */
    private val lastMovementTimes = ConcurrentHashMap<String, Long>()

    /** Bytes last reported per chunk, nested per item so an item's whole set can be dropped at
     * once. Lets [recordMovement] spot real progress without re-summing the chunk table on every
     * 16 KB callback. */
    private val lastChunkBytes = ConcurrentHashMap<String, ConcurrentHashMap<Long, Long>>()

    /** Total bytes an item had transferred when its stall clock was last reset — the mark the next
     * [STALL_MIN_PROGRESS_BYTES] is measured from. */
    private val stallBaselineValues = ConcurrentHashMap<String, Long>()

    /** Ids whose transfer was aborted by [hasStalled] rather than by the user, so the result can
     * be reported as a failure instead of a cancellation. */
    private val stalledIds = ConcurrentHashMap.newKeySet<String>()

    override fun observeItem(id: String): Flow<DownloadItem?> = downloadItemDao.getAsFlow(id)

    override fun observeAllItems(): Flow<List<DownloadItem>> = downloadItemDao.getAllAsFlow()

    override suspend fun getItem(id: String): DownloadItem? = downloadItemDao.get(id)

    override suspend fun queue(item: DownloadItem) {
        downloadItemDao.insert(item)
    }

    override suspend fun getOldestQueuedItem(): DownloadItem? = downloadItemDao.getOldestByState(
        DownloadItemState.QUEUED
    )

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
        clearRateHold(id)
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
        lastProgressWriteTimes.remove(id)
        lastProgressValues.remove(id)
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

        startStallWatch(id)

        val result = mediaTransferEngine.transfer(
            chunks = chunks,
            url = url,
            headers = headers,
            destinationFile = destinationFile,
            shouldInterrupt = { interruptFlags.containsKey(id) || hasStalled(id) },
        ) { chunkId, bytesDownloaded, status ->
            recordMovement(id, chunkId, bytesDownloaded)
            downloadChunkDao.updateProgress(chunkId, bytesDownloaded, status)
            writeAggregatedProgressThrottled(id, phase, totalBytes, status)
        }

        return resolveStalled(id, result)
    }

    override suspend fun runHlsTransfer(
        id: String,
        segments: List<HlsSegmentInfo>,
        startIndex: Int,
        headers: Map<String, String>,
        destinationFile: UniFile,
    ): MediaTransferResult {
        if (interruptFlags.containsKey(id)) return MediaTransferResult.Cancelled

        startStallWatch(id)

        val result = hlsTransferEngine.transfer(
            segments = segments,
            startIndex = startIndex,
            headers = headers,
            destinationFile = destinationFile,
            shouldInterrupt = { interruptFlags.containsKey(id) || hasStalled(id) },
        ) { segmentsWritten, totalSegments ->
            // Every callback here is a segment landing, so it is movement by definition — no need
            // to diff against a previous value the way the byte-range path does.
            lastMovementTimes[id] = System.currentTimeMillis()
            writeHlsProgressThrottled(id, segmentsWritten, totalSegments)
        }

        return resolveStalled(id, result)
    }

    private suspend fun writeHlsProgressThrottled(
        id: String,
        segmentsWritten: Int,
        totalSegments: Int,
    ) {
        val now = System.currentTimeMillis()
        val lastWrite = lastProgressWriteTimes[id] ?: 0L
        val isFinal = segmentsWritten >= totalSegments
        if (!isFinal && now - lastWrite < PROGRESS_WRITE_THROTTLE_MS) return

        val rate = smoothedRate(id, computeRate(id, segmentsWritten.toLong(), now, lastWrite))
        lastProgressWriteTimes[id] = now
        lastProgressValues[id] = segmentsWritten.toLong()
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
        val lastWrite = lastProgressWriteTimes[id] ?: 0L
        val shouldWrite = status != DownloadChunkStatus.DOWNLOADING || now - lastWrite >= PROGRESS_WRITE_THROTTLE_MS
        if (!shouldWrite) return

        val totalDownloaded = downloadChunkDao.getChunksForItem(id).sumOf { it.bytesDownloaded }
        val rate = smoothedRate(id, computeRate(id, totalDownloaded, now, lastWrite))
        lastProgressWriteTimes[id] = now
        lastProgressValues[id] = totalDownloaded

        if (phase == DownloadPhase.STREAM) {
            downloadItemDao.updateStreamProgress(id, totalDownloaded, totalBytes ?: 0, rate, Date())
        } else {
            downloadItemDao.updateDownloadRate(id, rate, Date())
        }
    }

    /** Bytes-(or segments-)per-second since the previous throttled write for [id], derived from
     * the same [lastProgressWriteTimes]/[lastProgressValues] bookkeeping the throttle itself
     * uses — no separate timing state needed. Zero on an item's first sample (nothing to diff
     * against yet, [lastWrite] is `0`) or if the clock hasn't meaningfully advanced. */
    private fun computeRate(
        id: String,
        currentValue: Long,
        now: Long,
        lastWrite: Long,
    ): Long {
        val lastValue = lastProgressValues[id] ?: return 0L
        val deltaMs = now - lastWrite
        if (lastWrite <= 0L || deltaMs <= 0L) return 0L

        val delta = (currentValue - lastValue).coerceAtLeast(0L)
        return (delta * 1000L) / deltaMs
    }

    /**
     * Smooths [rawRate] so a transfer that is alive but momentarily not moving — a slow HLS
     * segment, the hop between two subtitle files, a throttled window that happened to catch no
     * new bytes — doesn't immediately read as stopped. A zero sample replays the last measured
     * rate for up to [MAX_HELD_RATE_SAMPLES] consecutive writes; past that the transfer really has
     * stalled, so the hold is dropped and zero is reported honestly.
     */
    private fun smoothedRate(
        id: String,
        rawRate: Long,
    ): Long {
        if (rawRate > 0L) {
            lastNonZeroRates[id] = rawRate
            heldRateSampleCounts.remove(id)
            return rawRate
        }

        val held = lastNonZeroRates[id] ?: return 0L
        val timesHeld = heldRateSampleCounts[id] ?: 0
        if (timesHeld >= MAX_HELD_RATE_SAMPLES) {
            clearRateHold(id)
            return 0L
        }

        heldRateSampleCounts[id] = timesHeld + 1
        return held
    }

    private fun startStallWatch(id: String) {
        lastMovementTimes[id] = System.currentTimeMillis()
        stalledIds.remove(id)
        // Re-baseline: a resumed transfer picks up at the byte count the last one left off at, so
        // holding onto those figures would read its first callbacks as "no movement".
        lastChunkBytes.remove(id)
        stallBaselineValues.remove(id)
    }

    /**
     * Notes where [chunkId] has got to and, if the item as a whole has advanced far enough since
     * the last reset, restarts its stall clock.
     *
     * The bar is [STALL_MIN_PROGRESS_BYTES] rather than a single byte on purpose: a connection
     * dribbling out a handful of bytes a second keeps a byte-counting watchdog permanently happy
     * while still reporting 0 B/s and never finishing. Requiring one buffer's worth per window
     * makes "alive" mean actually moving. Chunks run concurrently and each reports its own running
     * total, so the item's progress is their sum — a chunk that finishes and stops reporting must
     * not read as a stall while its siblings are still going.
     */
    private fun recordMovement(
        id: String,
        chunkId: Long,
        bytesDownloaded: Long,
    ) {
        val perChunk = lastChunkBytes.getOrPut(id) { ConcurrentHashMap() }
        perChunk[chunkId] = bytesDownloaded

        val total = perChunk.values.sum()
        val baseline = stallBaselineValues[id]
        if (baseline != null && total - baseline < STALL_MIN_PROGRESS_BYTES) return

        stallBaselineValues[id] = total
        lastMovementTimes[id] = System.currentTimeMillis()
    }

    /**
     * Whether [id] has gone [STALL_TIMEOUT_MS] without the headway [recordMovement] asks for.
     * Polled from the engines' `shouldInterrupt`, which is the only hook that can unwind a
     * transfer from the inside; [resolveStalled] then re-labels the resulting cancellation as the
     * failure it really is.
     */
    private fun hasStalled(id: String): Boolean {
        val lastMovement = lastMovementTimes[id] ?: return false
        if (System.currentTimeMillis() - lastMovement < STALL_TIMEOUT_MS) return false

        stalledIds.add(id)
        return true
    }

    private fun resolveStalled(
        id: String,
        result: MediaTransferResult,
    ): MediaTransferResult {
        lastMovementTimes.remove(id)
        lastChunkBytes.remove(id)
        stallBaselineValues.remove(id)
        val stalled = stalledIds.remove(id)

        // A pause/stop that landed in the same window wins — the user asked for it, and reporting
        // their own tap back to them as a download error would be nonsense.
        if (!stalled || result !is MediaTransferResult.Cancelled || interruptFlags.containsKey(id)) {
            return result
        }

        return MediaTransferResult.Failed(
            IOException("Download stalled — no progress for ${STALL_TIMEOUT_MS / 1000} seconds"),
        )
    }

    private fun clearRateHold(id: String) {
        lastNonZeroRates.remove(id)
        heldRateSampleCounts.remove(id)
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
        lastProgressWriteTimes.remove(id)
        lastProgressValues.remove(id)
        clearRateHold(id)
        downloadItemDao.delete(id)
    }

    companion object {
        private const val PROGRESS_WRITE_THROTTLE_MS = 1000L
        private const val MAX_HELD_RATE_SAMPLES = 3

        /** Comfortably clear of OkHttp's 10s default read timeout and the engines' three retries,
         * so this only fires for a connection that stays open while going nowhere. */
        private const val STALL_TIMEOUT_MS = 60_000L

        /** One transfer buffer. Together with [STALL_TIMEOUT_MS] this sets the floor for "still
         * downloading" at roughly 273 B/s — dead by any standard for a video file, but far enough
         * below a genuinely slow connection not to fail one that is still making headway. */
        private const val STALL_MIN_PROGRESS_BYTES = 16L * 1024
    }
}
