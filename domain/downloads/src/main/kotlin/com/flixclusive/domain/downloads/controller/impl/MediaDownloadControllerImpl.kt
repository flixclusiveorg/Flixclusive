package com.flixclusive.domain.downloads.controller.impl

import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.database.entity.downloads.DownloadItem
import com.flixclusive.core.database.entity.downloads.DownloadItemState
import com.flixclusive.core.database.entity.downloads.DownloadPhase
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.datastore.model.user.DataPreferences
import com.flixclusive.core.datastore.model.user.UserPreferences
import com.flixclusive.data.downloads.directory.DownloadDirectoryRepository
import com.flixclusive.data.downloads.hls.HlsManifestResolver
import com.flixclusive.data.downloads.hls.HlsResolutionResult
import com.flixclusive.data.downloads.model.DownloadInterruptReason
import com.flixclusive.data.downloads.repository.MediaDownloadRepository
import com.flixclusive.data.downloads.transfer.MediaTransferResult
import com.flixclusive.data.downloads.util.DownloadPathUtil
import com.flixclusive.domain.downloads.controller.MediaDownloadController
import com.flixclusive.domain.downloads.controller.MediaDownloadServiceController
import com.flixclusive.domain.downloads.usecase.GetDownloadDirectoryUseCase
import com.hippo.unifile.UniFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class MediaDownloadControllerImpl @Inject constructor(
    private val mediaDownloadRepository: MediaDownloadRepository,
    private val downloadDirectoryRepository: DownloadDirectoryRepository,
    private val getDownloadDirectoryUseCase: GetDownloadDirectoryUseCase,
    private val hlsManifestResolver: HlsManifestResolver,
    private val mediaDownloadServiceController: MediaDownloadServiceController,
    private val dataStoreManager: DataStoreManager,
    private val appDispatchers: AppDispatchers,
) : MediaDownloadController {
    private val scope by lazy { CoroutineScope(appDispatchers.io + SupervisorJob()) }
    private val jobs = mutableMapOf<Long, Job>()

    private val dispatchMutex = Mutex()
    private val activeItemIds = mutableSetOf<Long>()

    override fun start(itemId: Long) {
        if (jobs[itemId]?.isActive == true) return
        jobs[itemId] = scope.launch { dispatchOrQueue(itemId) }
    }

    override fun resume(itemId: Long) = start(itemId)

    override fun retry(itemId: Long) {
        if (jobs[itemId]?.isActive == true) return
        jobs[itemId] = scope.launch {
            mediaDownloadRepository.resetChunks(itemId)
            mediaDownloadRepository.updateState(itemId, DownloadItemState.QUEUED, null)
            dispatchOrQueue(itemId)
        }
    }

    override fun pause(itemId: Long) {
        scope.launch {
            val item = mediaDownloadRepository.getItem(itemId) ?: return@launch
            when (item.state) {
                DownloadItemState.QUEUED -> mediaDownloadRepository.updateState(itemId, DownloadItemState.PAUSED, null)
                DownloadItemState.DOWNLOADING_STREAM, DownloadItemState.FETCHING_SUBTITLES ->
                    mediaDownloadRepository.requestInterrupt(itemId, DownloadInterruptReason.PAUSE)
                else -> Unit
            }
        }
    }

    override fun stop(itemId: Long) {
        scope.launch {
            val item = mediaDownloadRepository.getItem(itemId) ?: return@launch
            when (item.state) {
                DownloadItemState.DOWNLOADING_STREAM, DownloadItemState.FETCHING_SUBTITLES ->
                    mediaDownloadRepository.requestInterrupt(itemId, DownloadInterruptReason.STOP)
                DownloadItemState.COMPLETED, DownloadItemState.STOPPED -> Unit
                else -> stopInactiveItem(itemId, item)
            }
        }
    }

    override fun delete(itemId: Long) {
        scope.launch {
            val item = mediaDownloadRepository.getItem(itemId)
            if (item != null) {
                resolveDirectory(item)?.delete()
            }
            mediaDownloadRepository.resetChunks(itemId)
            mediaDownloadRepository.delete(itemId)
        }
    }

    override fun pauseBatch(
        mediaId: String,
        seasonNumber: Int,
    ) {
        scope.launch {
            mediaDownloadRepository.getBatch(mediaId, seasonNumber).forEach { pause(it.id) }
        }
    }

    override fun stopBatch(
        mediaId: String,
        seasonNumber: Int,
    ) {
        scope.launch {
            mediaDownloadRepository.getBatch(mediaId, seasonNumber).forEach { stop(it.id) }
        }
    }

    private suspend fun stopInactiveItem(
        itemId: Long,
        item: DownloadItem,
    ) {
        resolveDirectory(item)?.delete()
        mediaDownloadRepository.resetChunks(itemId)
        mediaDownloadRepository.updateState(itemId, DownloadItemState.STOPPED, null)
    }

    private suspend fun currentConcurrencyLimit(): Int =
        dataStoreManager
            .getUserPrefsAsFlow(UserPreferences.DATA_PREFS_KEY, DataPreferences::class)
            .first()
            .downloadConcurrencyLimit
            .coerceAtLeast(1)

    private suspend fun tryReserveSlot(itemId: Long): Boolean =
        dispatchMutex.withLock {
            if (itemId in activeItemIds || activeItemIds.size >= currentConcurrencyLimit()) {
                false
            } else {
                activeItemIds += itemId
                true
            }
        }

    private suspend fun releaseSlot(itemId: Long) {
        dispatchMutex.withLock { activeItemIds -= itemId }
    }

    /** Runs [itemId] now if a concurrency slot is free; otherwise it stays QUEUED for [dispatchNext] to pick up. */
    private suspend fun dispatchOrQueue(itemId: Long) {
        if (!tryReserveSlot(itemId)) return

        mediaDownloadServiceController.ensureRunning()

        try {
            runDownload(itemId)
        } finally {
            releaseSlot(itemId)
            dispatchNext()
        }
    }

    /** Fills every free concurrency slot with the oldest QUEUED items, FIFO, until none remain or the limit is hit. */
    private suspend fun dispatchNext() {
        while (true) {
            val next = dispatchMutex.withLock {
                if (activeItemIds.size >= currentConcurrencyLimit()) return
                val candidate = mediaDownloadRepository.getOldestQueuedItem() ?: return
                // Guards a theoretical race with dispatchOrQueue reserving the same id; bail rather
                // than spin, since a queued item can never legitimately already be active.
                if (!activeItemIds.add(candidate.id)) return
                candidate
            }

            mediaDownloadServiceController.ensureRunning()

            jobs[next.id] = scope.launch {
                try {
                    runDownload(next.id)
                } finally {
                    releaseSlot(next.id)
                    dispatchNext()
                }
            }
        }
    }

    private suspend fun runDownload(itemId: Long) {
        val item = mediaDownloadRepository.getItem(itemId) ?: return
        val streamUrl = item.streamUrl ?: return fail(itemId, "No stream link to download")
        val directory = resolveDirectory(item) ?: return fail(itemId, "Unable to access download folder")

        val resumingSubtitles = item.state == DownloadItemState.FETCHING_SUBTITLES ||
            (item.state == DownloadItemState.PAUSED && item.phase == DownloadPhase.SUBTITLES)

        if (resumingSubtitles) {
            runSubtitlePhase(itemId, item, directory)
        } else {
            runStreamPhase(itemId, item, directory, streamUrl)
        }
    }

    private suspend fun runStreamPhase(
        itemId: Long,
        item: DownloadItem,
        directory: UniFile,
        streamUrl: String,
    ) {
        mediaDownloadRepository.updateState(itemId, DownloadItemState.DOWNLOADING_STREAM, DownloadPhase.STREAM)

        if (item.isHlsStream) {
            return runHlsStreamPhase(itemId, item, directory, streamUrl)
        }

        val fileName = DownloadPathUtil.buildStreamFileName(
            DownloadPathUtil.buildFileTitle(item.mediaTitle, item.episodeTitle),
            DownloadPathUtil.extensionFromUrl(
                streamUrl,
                DownloadPathUtil.DEFAULT_STREAM_EXTENSION,
                DownloadPathUtil.STREAM_EXTENSIONS
            ),
        )
        val destinationFile = downloadDirectoryRepository.getOrCreateFile(directory, fileName)
            ?: return fail(itemId, "Unable to create destination file")

        val result = mediaDownloadRepository.runTransfer(
            id = itemId,
            phase = DownloadPhase.STREAM,
            url = streamUrl,
            headers = item.streamHeaders ?: emptyMap(),
            destinationFile = destinationFile,
            totalBytes = item.streamTotalBytes.takeIf { it > 0 },
        )

        handleStreamTransferResult(itemId, item, directory, destinationFile, result)
    }

    /**
     * HLS segments have no known byte length ahead of time, so this reuses a raw ".mp4" file name
     * (matching CS3's approach) instead of deriving an extension from the manifest URL, and reuses
     * [DownloadItem.streamBytesDownloaded]/[DownloadItem.streamTotalBytes] to mean segment counts
     * rather than byte counts for resume.
     */
    private suspend fun runHlsStreamPhase(
        itemId: Long,
        item: DownloadItem,
        directory: UniFile,
        streamUrl: String,
    ) {
        val headers = item.streamHeaders ?: emptyMap()

        val fileName = DownloadPathUtil.buildStreamFileName(
            DownloadPathUtil.buildFileTitle(item.mediaTitle, item.episodeTitle),
            DownloadPathUtil.DEFAULT_STREAM_EXTENSION,
        )
        val destinationFile = downloadDirectoryRepository.getOrCreateFile(directory, fileName)
            ?: return fail(itemId, "Unable to create destination file")

        val resolution = hlsManifestResolver.resolve(streamUrl, headers)
        val playlist = when (resolution) {
            is HlsResolutionResult.Success -> resolution.playlist
            is HlsResolutionResult.Failed -> return retryWithNextCandidateOrFail(
                itemId,
                item,
                directory,
                destinationFile,
                MediaTransferResult.Failed(IOException(resolution.reason))
            )
        }

        val result = mediaDownloadRepository.runHlsTransfer(
            id = itemId,
            segments = playlist.segments,
            startIndex = item.streamBytesDownloaded.toInt(),
            headers = headers,
            destinationFile = destinationFile,
        )

        handleStreamTransferResult(itemId, item, directory, destinationFile, result)
    }

    private suspend fun handleStreamTransferResult(
        itemId: Long,
        item: DownloadItem,
        directory: UniFile,
        destinationFile: UniFile,
        result: MediaTransferResult,
    ) {
        when (result) {
            is MediaTransferResult.Completed -> {
                if (destinationFile.length() < MIN_VALID_STREAM_FILE_BYTES) {
                    return fail(itemId, "Downloaded file is too small to be valid")
                }
                mediaDownloadRepository.updateState(itemId, DownloadItemState.STREAM_COMPLETE, null)
                advancePastStreamComplete(itemId, item, directory)
            }
            is MediaTransferResult.Cancelled -> handleInterrupted(itemId, DownloadPhase.STREAM, directory)
            is MediaTransferResult.Failed -> retryWithNextCandidateOrFail(
                itemId,
                item,
                directory,
                destinationFile,
                result
            )
        }
    }

    /**
     * On stream failure, falls through the candidate links persisted at queue time
     * ([DownloadItem.streamFallbackCandidates]) before giving up, since the link a
     * provider returns can go dead (expire, get throttled) independently of the app.
     */
    private suspend fun retryWithNextCandidateOrFail(
        itemId: Long,
        item: DownloadItem,
        directory: UniFile,
        staleDestinationFile: UniFile,
        result: MediaTransferResult.Failed,
    ) {
        val nextCandidate = mediaDownloadRepository.advanceStreamCandidate(itemId)
        if (nextCandidate == null) {
            return fail(itemId, result.cause.message ?: "Stream download failed")
        }

        staleDestinationFile.delete()
        mediaDownloadRepository.resetChunks(itemId)
        val refreshedItem = mediaDownloadRepository.getItem(itemId) ?: item
        runStreamPhase(itemId, refreshedItem, directory, nextCandidate.url)
    }

    private suspend fun advancePastStreamComplete(
        itemId: Long,
        item: DownloadItem,
        directory: UniFile,
    ) {
        if (item.subtitleUrl == null) {
            mediaDownloadRepository.updateState(itemId, DownloadItemState.COMPLETED, null)
            return
        }

        mediaDownloadRepository.resetChunks(itemId)
        runSubtitlePhase(itemId, item, directory)
    }

    private suspend fun runSubtitlePhase(
        itemId: Long,
        item: DownloadItem,
        directory: UniFile,
    ) {
        val subtitleUrl = item.subtitleUrl ?: run {
            mediaDownloadRepository.updateState(itemId, DownloadItemState.COMPLETED, null)
            return
        }

        mediaDownloadRepository.updateState(itemId, DownloadItemState.FETCHING_SUBTITLES, DownloadPhase.SUBTITLES)

        val subtitlesDirectory = downloadDirectoryRepository.getOrCreateSubtitlesDirectory(directory)
            ?: return markSubtitleFailureAndComplete(itemId, "Unable to create subtitles folder")

        val fileName = DownloadPathUtil.buildSubtitleFileName(
            DownloadPathUtil.buildFileTitle(item.mediaTitle, item.episodeTitle),
            DownloadPathUtil.extensionFromUrl(
                subtitleUrl,
                DownloadPathUtil.DEFAULT_SUBTITLE_EXTENSION,
                DownloadPathUtil.SUBTITLE_EXTENSIONS
            ),
        )
        val destinationFile = downloadDirectoryRepository.getOrCreateFile(subtitlesDirectory, fileName)
            ?: return markSubtitleFailureAndComplete(itemId, "Unable to create subtitle file")

        val result = mediaDownloadRepository.runTransfer(
            id = itemId,
            phase = DownloadPhase.SUBTITLES,
            url = subtitleUrl,
            headers = item.subtitleHeaders ?: emptyMap(),
            destinationFile = destinationFile,
            totalBytes = item.subtitleTotalBytes.takeIf { it > 0 },
        )

        when (result) {
            is MediaTransferResult.Completed -> mediaDownloadRepository.updateState(
                itemId,
                DownloadItemState.COMPLETED,
                null
            )
            is MediaTransferResult.Cancelled -> handleInterrupted(itemId, DownloadPhase.SUBTITLES, directory)
            is MediaTransferResult.Failed -> markSubtitleFailureAndComplete(
                itemId,
                result.cause.message ?: "Subtitle download failed"
            )
        }
    }

    private suspend fun handleInterrupted(
        itemId: Long,
        phase: DownloadPhase,
        directory: UniFile,
    ) {
        when (mediaDownloadRepository.consumeInterruptReason(itemId)) {
            DownloadInterruptReason.STOP -> {
                directory.delete()
                mediaDownloadRepository.resetChunks(itemId)
                mediaDownloadRepository.updateState(itemId, DownloadItemState.STOPPED, null)
            }
            else -> mediaDownloadRepository.updateState(itemId, DownloadItemState.PAUSED, phase)
        }
    }

    private suspend fun fail(
        itemId: Long,
        message: String,
    ) {
        mediaDownloadRepository.markError(itemId, message)
        mediaDownloadRepository.updateState(itemId, DownloadItemState.FAILED, null)
    }

    private suspend fun markSubtitleFailureAndComplete(
        itemId: Long,
        message: String,
    ) {
        mediaDownloadRepository.markSubtitleError(itemId, message)
        mediaDownloadRepository.updateState(itemId, DownloadItemState.COMPLETED, null)
    }

    private suspend fun resolveDirectory(item: DownloadItem): UniFile? =
        getDownloadDirectoryUseCase(item.mediaId, item.mediaTitle, item.seasonNumber, item.episodeNumber)

    companion object {
        // Catches a "successful" transfer that actually saved an error page or empty response
        // (e.g. a dead link the initial probe didn't catch) instead of a real video file.
        private const val MIN_VALID_STREAM_FILE_BYTES = 100 * 1024L
    }
}
