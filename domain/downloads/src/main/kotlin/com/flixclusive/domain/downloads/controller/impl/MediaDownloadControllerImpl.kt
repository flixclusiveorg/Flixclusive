package com.flixclusive.domain.downloads.controller.impl

import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.database.entity.downloads.DownloadItem
import com.flixclusive.core.database.entity.downloads.DownloadItemState
import com.flixclusive.core.database.entity.downloads.DownloadPhase
import com.flixclusive.data.downloads.directory.DownloadDirectoryRepository
import com.flixclusive.data.downloads.model.DownloadInterruptReason
import com.flixclusive.data.downloads.repository.MediaDownloadRepository
import com.flixclusive.data.downloads.transfer.MediaTransferResult
import com.flixclusive.data.downloads.util.DownloadPathUtil
import com.flixclusive.domain.downloads.controller.MediaDownloadController
import com.flixclusive.domain.downloads.usecase.GetDownloadDirectoryUseCase
import com.hippo.unifile.UniFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class MediaDownloadControllerImpl @Inject constructor(
    private val mediaDownloadRepository: MediaDownloadRepository,
    private val downloadDirectoryRepository: DownloadDirectoryRepository,
    private val getDownloadDirectoryUseCase: GetDownloadDirectoryUseCase,
    private val appDispatchers: AppDispatchers,
) : MediaDownloadController {
    private val scope by lazy { CoroutineScope(appDispatchers.io + SupervisorJob()) }
    private val jobs = mutableMapOf<Long, Job>()

    override fun start(itemId: Long) {
        if (jobs[itemId]?.isActive == true) return
        jobs[itemId] = scope.launch { runDownload(itemId) }
    }

    override fun resume(itemId: Long) = start(itemId)

    override fun retry(itemId: Long) {
        if (jobs[itemId]?.isActive == true) return
        jobs[itemId] = scope.launch {
            mediaDownloadRepository.resetChunks(itemId)
            mediaDownloadRepository.updateState(itemId, DownloadItemState.QUEUED, null)
            runDownload(itemId)
        }
    }

    override fun pause(itemId: Long) {
        mediaDownloadRepository.requestInterrupt(itemId, DownloadInterruptReason.PAUSE)
    }

    override fun stop(itemId: Long) {
        mediaDownloadRepository.requestInterrupt(itemId, DownloadInterruptReason.STOP)
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

        val fileName = DownloadPathUtil.buildStreamFileName(
            DownloadPathUtil.buildFileTitle(item.mediaTitle, item.episodeTitle),
            DownloadPathUtil.extensionFromUrl(streamUrl, DEFAULT_STREAM_EXTENSION, STREAM_EXTENSIONS),
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

        when (result) {
            is MediaTransferResult.Completed -> {
                mediaDownloadRepository.updateState(itemId, DownloadItemState.STREAM_COMPLETE, null)
                advancePastStreamComplete(itemId, item, directory)
            }
            is MediaTransferResult.Cancelled -> handleInterrupted(itemId, DownloadPhase.STREAM, directory)
            is MediaTransferResult.Failed -> fail(itemId, result.cause.message ?: "Stream download failed")
        }
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
            DownloadPathUtil.extensionFromUrl(subtitleUrl, DEFAULT_SUBTITLE_EXTENSION, SUBTITLE_EXTENSIONS),
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
        private val STREAM_EXTENSIONS = setOf("mp4", "mkv", "mov", "webm")
        private const val DEFAULT_STREAM_EXTENSION = "mp4"
        private val SUBTITLE_EXTENSIONS = setOf("srt", "vtt", "ass", "ssa")
        private const val DEFAULT_SUBTITLE_EXTENSION = "srt"
    }
}
