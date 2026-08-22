package com.flixclusive.data.downloads.repository.impl

import com.flixclusive.core.database.entity.downloads.DownloadPhase
import com.flixclusive.data.downloads.transfer.MediaTransferResult
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

/**
 * The in-memory bookkeeping behind a download's reported speed and its stall watchdog.
 *
 * Split out of [MediaDownloadRepositoryImpl] so the repository is left as a persistence layer:
 * none of this is ever written to the database except as a rate on an already-throttled progress
 * write, and all of it is per-process state that dies with the app.
 */
internal class TransferProgressTracker {
    private val lastProgressWriteTimes = ConcurrentHashMap<String, Long>()

    /** The bytes-(or segments-)downloaded value written at [lastProgressWriteTimes]'s timestamp for
     * the same id — the previous sample [computeRate] diffs against. */
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

    fun lastWriteAt(id: String): Long = lastProgressWriteTimes[id] ?: 0L

    fun lastMeasuredRate(id: String): Long = lastNonZeroRates[id] ?: 0L

    fun recordWrite(
        id: String,
        now: Long,
        value: Long,
    ) {
        lastProgressWriteTimes[id] = now
        lastProgressValues[id] = value
    }

    /** The rate to report for this write: measured against the previous sample, then smoothed. */
    fun rateFor(
        id: String,
        currentValue: Long,
        now: Long,
        lastWrite: Long,
    ): Long = smoothedRate(id, computeRate(id, currentValue, now, lastWrite))

    /**
     * Drops the sample a rate would be diffed against, without touching the hold-over.
     *
     * Chunks being deleted means any in-flight sample no longer has a valid baseline — the first
     * write of the restarted transfer would otherwise diff against bytes and timing from a
     * since-discarded run and read as one huge, stale-timed sample.
     */
    fun clearRateBaseline(id: String) {
        lastProgressWriteTimes.remove(id)
        lastProgressValues.remove(id)
    }

    fun clearRateHold(id: String) {
        lastNonZeroRates.remove(id)
        heldRateSampleCounts.remove(id)
    }

    fun forget(id: String) {
        clearRateBaseline(id)
        clearRateHold(id)
    }

    fun startStallWatch(id: String) {
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
    fun recordMovement(
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
     * Restarts [id]'s stall clock unconditionally.
     *
     * For HLS, where every callback is a whole segment landing and so is movement by definition —
     * there is nothing to diff against the way [recordMovement] does for byte ranges.
     */
    fun markMoved(id: String) {
        lastMovementTimes[id] = System.currentTimeMillis()
    }

    /**
     * Whether [id] has gone [STALL_TIMEOUT_MS] without the headway [recordMovement] asks for.
     * Polled from the engines' `shouldInterrupt`, which is the only hook that can unwind a
     * transfer from the inside; [resolveStalled] then re-labels the resulting cancellation as the
     * failure it really is.
     */
    fun hasStalled(
        id: String,
        phase: DownloadPhase,
    ): Boolean {
        // Subtitles are exempt: the whole file is often smaller than the throughput floor this
        // watchdog demands, so a slow-but-fine subtitle would trip it on size alone. OkHttp's read
        // timeout is enough for something that small.
        if (phase == DownloadPhase.SUBTITLES) return false

        val lastMovement = lastMovementTimes[id] ?: return false
        if (System.currentTimeMillis() - lastMovement < STALL_TIMEOUT_MS) return false

        stalledIds.add(id)
        return true
    }

    /**
     * Re-labels a cancellation this watchdog caused as the failure it really is.
     *
     * [interrupted] is passed in rather than read here: the interrupt flags are the repository's
     * mechanism, and a pause or stop that landed in the same window wins — the user asked for it,
     * and reporting their own tap back to them as a download error would be nonsense.
     */
    fun resolveStalled(
        id: String,
        result: MediaTransferResult,
        interrupted: Boolean,
    ): MediaTransferResult {
        lastMovementTimes.remove(id)
        lastChunkBytes.remove(id)
        stallBaselineValues.remove(id)
        val stalled = stalledIds.remove(id)

        if (!stalled || result !is MediaTransferResult.Cancelled || interrupted) {
            return result
        }

        return MediaTransferResult.Failed(
            IOException("Download stalled — no progress for ${STALL_TIMEOUT_MS / 1000} seconds"),
        )
    }

    /** Zero on an item's first sample (nothing to diff against yet, [lastWrite] is `0`) or if the
     * clock hasn't meaningfully advanced. */
    private fun computeRate(
        id: String,
        currentValue: Long,
        now: Long,
        lastWrite: Long,
    ): Long {
        val lastValue = lastProgressValues[id] ?: return 0L
        if (lastWrite <= 0L) return 0L

        val deltaMs = now - lastWrite
        if (deltaMs <= 0L) return 0L

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

    companion object {
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
