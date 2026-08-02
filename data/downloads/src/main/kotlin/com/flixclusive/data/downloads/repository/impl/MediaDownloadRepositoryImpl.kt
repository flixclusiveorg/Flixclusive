package com.flixclusive.data.downloads.repository.impl

import com.flixclusive.core.database.dao.downloads.DownloadChunkDao
import com.flixclusive.core.database.dao.downloads.DownloadItemDao
import com.flixclusive.core.database.entity.downloads.DownloadChunk
import com.flixclusive.core.database.entity.downloads.DownloadChunkStatus
import com.flixclusive.core.database.entity.downloads.DownloadItem
import com.flixclusive.core.database.entity.downloads.DownloadItemState
import com.flixclusive.core.database.entity.downloads.DownloadPhase
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
