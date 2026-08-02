package com.flixclusive.data.downloads.repository.impl

import com.flixclusive.core.database.dao.downloads.DownloadChunkDao
import com.flixclusive.core.database.dao.downloads.DownloadItemDao
import com.flixclusive.core.database.entity.downloads.DownloadChunk
import com.flixclusive.core.database.entity.downloads.DownloadChunkStatus
import com.flixclusive.core.database.entity.downloads.DownloadItem
import com.flixclusive.core.database.entity.downloads.DownloadItemState
import com.flixclusive.core.database.entity.downloads.DownloadPhase
import com.flixclusive.core.database.entity.downloads.DownloadStreamCandidate
import com.flixclusive.data.downloads.hls.HlsSegmentInfo
import com.flixclusive.data.downloads.hls.HlsTransferEngine
import com.flixclusive.data.downloads.model.DownloadInterruptReason
import com.flixclusive.data.downloads.repository.MediaDownloadRepository
import com.flixclusive.data.downloads.transfer.MediaTransferEngine
import com.flixclusive.data.downloads.transfer.MediaTransferResult
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
    private val interruptFlags = ConcurrentHashMap<Long, DownloadInterruptReason>()
    private val lastProgressWriteTimes = ConcurrentHashMap<Long, Long>()

    override fun observeItem(id: Long): Flow<DownloadItem?> = downloadItemDao.getAsFlow(id)

    override fun observeAllItems(): Flow<List<DownloadItem>> = downloadItemDao.getAllAsFlow()

    override suspend fun getItem(id: Long): DownloadItem? = downloadItemDao.get(id)

    override suspend fun queue(item: DownloadItem): Long = downloadItemDao.insert(item)

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

    override suspend fun updateState(
        id: Long,
        state: DownloadItemState,
        phase: DownloadPhase?,
    ) {
        downloadItemDao.updateState(id, state, phase, Date())
    }

    override suspend fun markError(
        id: Long,
        message: String,
    ) {
        downloadItemDao.updateError(id, message, Date())
    }

    override suspend fun markSubtitleError(
        id: Long,
        message: String,
    ) {
        downloadItemDao.updateSubtitleError(id, message, Date())
    }

    override suspend fun resetChunks(id: Long) {
        downloadChunkDao.deleteChunksForItem(id)
        // Also zeroes the segment-count progress HLS items keep in the same columns, so a manual
        // retry restarts an HLS download instead of silently resuming it (matching the byte-range
        // engine, where deleting chunks already forces a from-scratch replan).
        downloadItemDao.updateStreamProgress(id, 0, 0, Date())
    }

    override suspend fun advanceStreamCandidate(id: Long): DownloadStreamCandidate? {
        val item = downloadItemDao.get(id) ?: return null
        val fallbackCandidates = item.streamFallbackCandidates ?: return null
        val candidate = fallbackCandidates.firstOrNull() ?: return null
        val remaining = fallbackCandidates.drop(1)

        downloadItemDao.updateStreamSource(id, candidate.url, candidate.headers, remaining, candidate.isHls, Date())
        return candidate
    }

    override suspend fun runTransfer(
        id: Long,
        phase: DownloadPhase,
        url: String,
        headers: Map<String, String>,
        destinationFile: UniFile,
        totalBytes: Long?,
    ): MediaTransferResult {
        interruptFlags.remove(id)

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

        return mediaTransferEngine.transfer(
            chunks = chunks,
            url = url,
            headers = headers,
            destinationFile = destinationFile,
            shouldInterrupt = { interruptFlags.containsKey(id) },
        ) { chunkId, bytesDownloaded, status ->
            downloadChunkDao.updateProgress(chunkId, bytesDownloaded, status)
            writeAggregatedProgressThrottled(id, phase, totalBytes, status)
        }
    }

    override suspend fun runHlsTransfer(
        id: Long,
        segments: List<HlsSegmentInfo>,
        startIndex: Int,
        headers: Map<String, String>,
        destinationFile: UniFile,
    ): MediaTransferResult {
        interruptFlags.remove(id)

        return hlsTransferEngine.transfer(
            segments = segments,
            startIndex = startIndex,
            headers = headers,
            destinationFile = destinationFile,
            shouldInterrupt = { interruptFlags.containsKey(id) },
        ) { segmentsWritten, totalSegments ->
            writeHlsProgressThrottled(id, segmentsWritten, totalSegments)
        }
    }

    private suspend fun writeHlsProgressThrottled(
        id: Long,
        segmentsWritten: Int,
        totalSegments: Int,
    ) {
        val now = System.currentTimeMillis()
        val lastWrite = lastProgressWriteTimes[id] ?: 0L
        val isFinal = segmentsWritten >= totalSegments
        if (!isFinal && now - lastWrite < PROGRESS_WRITE_THROTTLE_MS) return

        lastProgressWriteTimes[id] = now
        downloadItemDao.updateStreamProgress(id, segmentsWritten.toLong(), totalSegments.toLong(), Date())
    }

    private suspend fun writeAggregatedProgressThrottled(
        id: Long,
        phase: DownloadPhase,
        totalBytes: Long?,
        status: DownloadChunkStatus,
    ) {
        val now = System.currentTimeMillis()
        val lastWrite = lastProgressWriteTimes[id] ?: 0L
        val shouldWrite = status != DownloadChunkStatus.DOWNLOADING || now - lastWrite >= PROGRESS_WRITE_THROTTLE_MS
        if (!shouldWrite) return

        lastProgressWriteTimes[id] = now
        val totalDownloaded = downloadChunkDao.getChunksForItem(id).sumOf { it.bytesDownloaded }
        val updatedAt = Date()

        when (phase) {
            DownloadPhase.STREAM -> downloadItemDao.updateStreamProgress(
                id,
                totalDownloaded,
                totalBytes ?: 0,
                updatedAt
            )
            DownloadPhase.SUBTITLES -> downloadItemDao.updateSubtitleProgress(
                id,
                totalDownloaded,
                totalBytes ?: 0,
                updatedAt
            )
        }
    }

    override fun requestInterrupt(
        id: Long,
        reason: DownloadInterruptReason,
    ) {
        interruptFlags[id] = reason
    }

    override fun consumeInterruptReason(id: Long): DownloadInterruptReason? = interruptFlags.remove(id)

    override suspend fun delete(id: Long) {
        lastProgressWriteTimes.remove(id)
        downloadItemDao.delete(id)
    }

    companion object {
        private const val PROGRESS_WRITE_THROTTLE_MS = 1000L
    }
}
