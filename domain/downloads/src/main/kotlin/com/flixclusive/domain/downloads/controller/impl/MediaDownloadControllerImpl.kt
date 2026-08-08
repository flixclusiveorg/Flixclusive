package com.flixclusive.domain.downloads.controller.impl

import android.content.Context
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.common.domain.Async
import com.flixclusive.core.database.entity.downloads.DownloadItem
import com.flixclusive.core.database.entity.downloads.DownloadItemState
import com.flixclusive.core.database.entity.downloads.DownloadPhase
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.datastore.model.user.DataPreferences
import com.flixclusive.core.datastore.model.user.UserPreferences
import com.flixclusive.core.datastore.model.user.download.DownloadLinkSortDirection
import com.flixclusive.core.network.monitor.NetworkMonitor
import com.flixclusive.data.downloads.directory.DownloadDirectoryRepository
import com.flixclusive.data.downloads.hls.HlsManifestResolver
import com.flixclusive.data.downloads.hls.HlsResolutionResult
import com.flixclusive.data.downloads.model.DownloadInterruptReason
import com.flixclusive.data.downloads.repository.MediaDownloadRepository
import com.flixclusive.data.downloads.transfer.MediaTransferResult
import com.flixclusive.data.downloads.transfer.TransferFailure
import com.flixclusive.data.downloads.util.DownloadPathUtil
import com.flixclusive.data.provider.repository.MediaLinksRepository
import com.flixclusive.domain.downloads.controller.MediaDownloadController
import com.flixclusive.domain.downloads.controller.MediaDownloadServiceController
import com.flixclusive.domain.downloads.usecase.GetDownloadDirectoryUseCase
import com.flixclusive.domain.downloads.usecase.RankedDownloadCandidate
import com.flixclusive.domain.downloads.usecase.ResolveDownloadableStreamUseCase
import com.hippo.unifile.UniFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class MediaDownloadControllerImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val mediaDownloadRepository: MediaDownloadRepository,
    private val mediaLinksRepository: MediaLinksRepository,
    private val resolveDownloadableStreamUseCase: ResolveDownloadableStreamUseCase,
    private val downloadDirectoryRepository: DownloadDirectoryRepository,
    private val getDownloadDirectoryUseCase: GetDownloadDirectoryUseCase,
    private val hlsManifestResolver: HlsManifestResolver,
    private val networkMonitor: NetworkMonitor,
    private val mediaDownloadServiceController: MediaDownloadServiceController,
    private val dataStoreManager: DataStoreManager,
    private val appDispatchers: AppDispatchers,
) : MediaDownloadController {
    private val scope by lazy { CoroutineScope(appDispatchers.io + SupervisorJob()) }
    private val jobs = ConcurrentHashMap<String, Job>()

    private val dispatchMutex = Mutex()
    private val activeItemIds = mutableSetOf<String>()

    override fun start(itemId: String) {
        if (jobs[itemId]?.isActive == true) return
        jobs[itemId] = scope.launch { dispatchOrQueue(itemId) }
    }

    override fun resume(itemId: String) = start(itemId)

    override fun retry(itemId: String) {
        if (jobs[itemId]?.isActive == true) return
        jobs[itemId] = scope.launch {
            mediaDownloadRepository.resetChunks(itemId)
            mediaDownloadRepository.updateState(itemId, DownloadItemState.QUEUED, null)
            dispatchOrQueue(itemId)
        }
    }

    override fun resumeInterrupted() {
        scope.launch {
            // Left as-is rather than requeued when held back: the rows stay in whatever state the
            // dead process left them, and the next sweep on an unmetered connection recovers them.
            if (!isAutoStartAllowed()) return@launch

            val live = dispatchMutex.withLock { activeItemIds.toList() }
            mediaDownloadRepository.requeueInterruptedItems(live)
            // Unconditional: the sweep may have found nothing, but plain QUEUED rows that never got
            // to start before the process died still need picking up.
            dispatchNext()
        }
    }

    override fun pause(itemId: String) {
        scope.launch {
            val item = mediaDownloadRepository.getItem(itemId) ?: return@launch
            if (item.state.isTerminal || item.state == DownloadItemState.PAUSED) return@launch

            if (isDispatched(itemId)) {
                mediaDownloadRepository.requestInterrupt(itemId, DownloadInterruptReason.PAUSE)
            } else {
                mediaDownloadRepository.updateState(itemId, DownloadItemState.PAUSED, resumePhaseOf(item))
            }
        }
    }

    /**
     * The phase [item] should pick back up at. Only [DownloadItemState.STREAM_COMPLETE] needs
     * translating: it carries no phase of its own, but its video is already fully written, so
     * resuming it as anything other than subtitles would refetch the whole file.
     */
    private fun resumePhaseOf(item: DownloadItem): DownloadPhase? =
        if (item.state == DownloadItemState.STREAM_COMPLETE) DownloadPhase.SUBTITLES else item.phase

    override fun stop(itemId: String) {
        scope.launch {
            val item = mediaDownloadRepository.getItem(itemId) ?: return@launch
            if (item.state == DownloadItemState.COMPLETED || item.state == DownloadItemState.STOPPED) {
                return@launch
            }

            if (isDispatched(itemId)) {
                mediaDownloadRepository.requestInterrupt(itemId, DownloadInterruptReason.STOP)
            } else {
                stopInactiveItem(itemId, item)
            }
        }
    }

    /**
     * Whether a coroutine is actually driving [itemId] right now. Both interrupt paths key off this
     * rather than off the persisted state, because the two disagree in each direction: a row can
     * read DOWNLOADING_STREAM with nothing running (the process died mid-transfer, and the
     * cooperative interrupt flag would have no one to poll it — the item would freeze), and a row
     * can read QUEUED while a coroutine is already resolving its links (where tearing the directory
     * down underneath it would just get overwritten by the still-running transfer).
     */
    private suspend fun isDispatched(itemId: String): Boolean = dispatchMutex.withLock { itemId in activeItemIds }

    override fun delete(itemId: String) {
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
        itemId: String,
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

    /**
     * Whether a download is allowed to start *on its own* right now. Gates only the automatic
     * paths — the post-force-close sweep and the queue dispatcher. An explicit start/resume/retry
     * deliberately bypasses this: a tap that silently does nothing is worse than the data it spends.
     */
    private suspend fun isAutoStartAllowed(): Boolean {
        val wifiOnly = dataStoreManager
            .getUserPrefsAsFlow(UserPreferences.DATA_PREFS_KEY, DataPreferences::class)
            .first()
            .downloadOnWifiOnly

        return !wifiOnly || !networkMonitor.isMetered.first()
    }

    private suspend fun currentLinkSortDirection(): DownloadLinkSortDirection =
        dataStoreManager
            .getUserPrefsAsFlow(UserPreferences.DATA_PREFS_KEY, DataPreferences::class)
            .first()
            .downloadLinkSortDirection

    private suspend fun tryReserveSlot(itemId: String): Boolean =
        dispatchMutex.withLock {
            if (itemId in activeItemIds || activeItemIds.size >= currentConcurrencyLimit()) {
                false
            } else {
                activeItemIds += itemId
                true
            }
        }

    private suspend fun releaseSlot(itemId: String) {
        dispatchMutex.withLock { activeItemIds -= itemId }
    }

    /** Runs [itemId] now if a concurrency slot is free; otherwise it stays QUEUED for [dispatchNext] to pick up. */
    private suspend fun dispatchOrQueue(itemId: String) {
        if (!tryReserveSlot(itemId)) return

        mediaDownloadServiceController.ensureRunning()

        try {
            runDownload(itemId)
        } finally {
            releaseSlot(itemId)
            jobs.remove(itemId)
            dispatchNext()
        }
    }

    /** Fills every free concurrency slot with the oldest QUEUED items, FIFO, until none remain or the limit is hit. */
    private suspend fun dispatchNext() {
        if (!isAutoStartAllowed()) return

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
                    jobs.remove(next.id)
                    dispatchNext()
                }
            }
        }
    }

    private suspend fun runDownload(itemId: String) {
        val item = mediaDownloadRepository.getItem(itemId) ?: return
        // A stop that landed while this dispatch was still waiting for its concurrency slot must
        // not be undone by it. Narrow to STOPPED on purpose: FAILED and COMPLETED are legitimate
        // states to start from (retry() requeues through here), STOPPED is the one the user
        // explicitly asked to end.
        if (item.state == DownloadItemState.STOPPED) return

        val directory = resolveDirectory(item) ?: return fail(itemId, "Unable to access download folder")

        // Phase-, not state-driven, so it also covers an item requeued to QUEUED by
        // resumeInterrupted() after the process died: its video is already fully written, and
        // re-entering the stream phase would download the whole thing again.
        val resumingSubtitles = item.state == DownloadItemState.FETCHING_SUBTITLES ||
            item.phase == DownloadPhase.SUBTITLES

        if (resumingSubtitles) {
            return runSubtitlePhase(itemId, item, directory)
        }

        val readyItem = if (item.sourceUrl == null) resolveSource(itemId, item) ?: return else item
        val sourceUrl = readyItem.sourceUrl ?: return fail(itemId, "No stream link to download")

        runStreamPhase(itemId, readyItem, directory, sourceUrl)
    }

    /**
     * Resolves the best reachable cached link for [item]'s media/episode and persists it as
     * [DownloadItem.sourceUrl]/[DownloadItem.isHlsStream] (kept in lockstep by a single repository
     * call). Marks [itemId] [DownloadItemState.FAILED] and returns `null` when nothing reachable
     * remains in the cache — the download engine never re-invokes the provider itself.
     */
    private suspend fun resolveSource(
        itemId: String,
        item: DownloadItem,
    ): DownloadItem? {
        val resolved = resolveDownloadableStreamUseCase(
            ownerId = item.ownerId,
            mediaId = item.mediaId,
            seasonNumber = item.seasonNumber,
            episodeNumber = item.episodeNumber,
        )

        if (resolved is Async.Failure) {
            fail(itemId, resolved.message.asString(context))
            return null
        }

        val candidate = (resolved as Async.Success<RankedDownloadCandidate>).data
        // HLS totals are tracked as segment counts once the manifest resolves, not bytes, so
        // only a direct file's probed content length seeds streamTotalBytes here.
        val totalBytes = if (candidate.isHls) 0L else candidate.contentLength ?: 0L
        mediaDownloadRepository.updateSource(itemId, candidate.stream.url, candidate.isHls, totalBytes)
        return mediaDownloadRepository.getItem(itemId)
    }

    private suspend fun headersForSource(
        item: DownloadItem,
        sourceUrl: String,
    ): Map<String, String> = mediaLinksRepository
        .getLinks(
            ownerId = item.ownerId,
            mediaId = item.mediaId,
            episodeNumber = item.episodeNumber,
            seasonNumber = item.seasonNumber,
        ).flatMap { it.streams }
        .firstOrNull { it.url == sourceUrl }
        ?.customHeaders
        ?: emptyMap()

    private suspend fun runStreamPhase(
        itemId: String,
        item: DownloadItem,
        directory: UniFile,
        sourceUrl: String,
    ) {
        // Link resolution and HLS manifest fetching happen before any transfer starts polling the
        // interrupt flag, so a stop requested during that window is caught here instead.
        mediaDownloadRepository.consumeInterruptReason(itemId)?.let { pendingInterrupt ->
            return applyInterrupt(itemId, pendingInterrupt, DownloadPhase.STREAM, directory)
        }

        mediaDownloadRepository.updateState(itemId, DownloadItemState.DOWNLOADING_STREAM, DownloadPhase.STREAM)

        if (item.isHlsStream) {
            return runHlsStreamPhase(itemId, item, directory, sourceUrl)
        }

        val fileName = DownloadPathUtil.buildStreamFileName(
            DownloadPathUtil.buildFileTitle(item.mediaTitle),
            DownloadPathUtil.extensionFromUrl(
                sourceUrl,
                DownloadPathUtil.DEFAULT_STREAM_EXTENSION,
                DownloadPathUtil.STREAM_EXTENSIONS
            ),
        )
        val destinationFile = resolveOrCreateStreamFile(itemId, item, directory, fileName)
            ?: return fail(itemId, "Unable to create destination file")

        val result = mediaDownloadRepository.runTransfer(
            id = itemId,
            phase = DownloadPhase.STREAM,
            url = sourceUrl,
            headers = headersForSource(item, sourceUrl),
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
        itemId: String,
        item: DownloadItem,
        directory: UniFile,
        sourceUrl: String,
    ) {
        val headers = headersForSource(item, sourceUrl)

        val fileName = DownloadPathUtil.buildStreamFileName(
            DownloadPathUtil.buildFileTitle(item.mediaTitle),
            DownloadPathUtil.DEFAULT_STREAM_EXTENSION,
        )
        val destinationFile = resolveOrCreateStreamFile(itemId, item, directory, fileName)
            ?: return fail(itemId, "Unable to create destination file")

        val resolution = hlsManifestResolver.resolve(sourceUrl, headers, currentLinkSortDirection())
        val playlist = when (resolution) {
            is HlsResolutionResult.Success -> resolution.playlist
            is HlsResolutionResult.Failed -> return markDeadAndRetryOrFail(
                itemId,
                item,
                directory,
                destinationFile,
                MediaTransferResult.Failed(IOException(resolution.reason))
            )
        }

        // A resumed HLS download indexes into a manifest resolved just now, not the one it started
        // against. If the playlist has been re-cut since — a different rendition, a rotating or
        // live playlist — the old segment index points somewhere else entirely and appending from
        // it would splice two different streams together. A changed segment count is the cheapest
        // reliable signal that happened; start over when it differs.
        val previousSegmentCount = item.streamTotalBytes
        val playlistChanged = previousSegmentCount > 0 && previousSegmentCount != playlist.segments.size.toLong()

        // Recreated rather than just emptied: the HLS engine appends, so the partial segments
        // already in the file would otherwise be spliced in front of the restarted download.
        val transferFile = if (playlistChanged) {
            mediaDownloadRepository.resetChunks(itemId)
            destinationFile.delete()
            val recreated = downloadDirectoryRepository.getOrCreateFile(directory, fileName)
                ?: return fail(itemId, "Unable to create destination file")
            mediaDownloadRepository.updateStreamFilePath(itemId, recreated.uri.toString())
            recreated
        } else {
            destinationFile
        }

        val result = mediaDownloadRepository.runHlsTransfer(
            id = itemId,
            segments = playlist.segments,
            startIndex = if (playlistChanged) 0 else item.streamBytesDownloaded.toInt(),
            headers = headers,
            destinationFile = transferFile,
        )

        handleStreamTransferResult(itemId, item, directory, transferFile, result)
    }

    /** Resolves [DownloadItem.streamFilePath] back to its [UniFile] on resume; otherwise creates
     * [fileName] fresh and persists its resulting document URI so future resumes/playback can
     * locate it without re-deriving the name from the (possibly since-changed) source link. */
    private suspend fun resolveOrCreateStreamFile(
        itemId: String,
        item: DownloadItem,
        directory: UniFile,
        fileName: String,
    ): UniFile? {
        item.streamFilePath?.let { path ->
            val existing = downloadDirectoryRepository.resolveFile(path)
            if (existing != null) {
                // The chunk rows claim bytes that have to actually be on disk for a resume to write
                // at the right offsets. If the file was deleted from under us and recreated, or
                // truncated, resuming would write into a hole and silently produce a broken video.
                if (existing.length() < item.streamBytesDownloaded) {
                    mediaDownloadRepository.resetChunks(itemId)
                }
                return existing
            }

            // The file the progress refers to is gone entirely — start the transfer over rather
            // than resuming into a freshly created, empty one.
            mediaDownloadRepository.resetChunks(itemId)
        }

        val destinationFile = downloadDirectoryRepository.getOrCreateFile(directory, fileName) ?: return null
        mediaDownloadRepository.updateStreamFilePath(itemId, destinationFile.uri.toString())
        return destinationFile
    }

    private suspend fun handleStreamTransferResult(
        itemId: String,
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
            is MediaTransferResult.Failed -> markDeadAndRetryOrFail(
                itemId,
                item,
                directory,
                destinationFile,
                result
            )
        }
    }

    /**
     * On stream failure, marks the link behind [DownloadItem.sourceUrl] dead in the link cache —
     * so it stops being re-probed later — then clears [DownloadItem.sourceUrl]/[DownloadItem.streamFilePath]
     * and re-enters resolution to rank the surviving cached links. Fails the item outright once the
     * resolve use case reports nothing reachable remains.
     */
    private suspend fun markDeadAndRetryOrFail(
        itemId: String,
        item: DownloadItem,
        directory: UniFile,
        staleDestinationFile: UniFile,
        result: MediaTransferResult.Failed,
    ) {
        // Without this, a stop is ignored for as long as it takes to walk every surviving cached
        // link — each failure re-enters runStreamPhase, and only a live transfer polls the flag.
        mediaDownloadRepository.consumeInterruptReason(itemId)?.let { pendingInterrupt ->
            return applyInterrupt(itemId, pendingInterrupt, DownloadPhase.STREAM, directory)
        }

        // A full disk, a revoked folder permission or a dropped connection would fail identically
        // against every other candidate — walking the list would just blacklist all of them and
        // leave a later retry with nothing to try. Stop here and keep the link cache intact.
        if (TransferFailure.of(result.cause) == TransferFailure.ENVIRONMENT) {
            return fail(itemId, result.cause.message ?: "Download failed")
        }

        item.sourceUrl?.let { deadUrl ->
            mediaLinksRepository.setLinkStatus(deadUrl, item.ownerId, isDead = true)
        }

        staleDestinationFile.delete()
        mediaDownloadRepository.resetChunks(itemId)
        mediaDownloadRepository.updateSource(itemId, sourceUrl = null, isHls = false, totalBytes = 0)
        mediaDownloadRepository.updateStreamFilePath(itemId, null)

        val clearedItem = mediaDownloadRepository.getItem(itemId) ?: return
        val resolvedItem = resolveSource(itemId, clearedItem) ?: return
        val sourceUrl = resolvedItem.sourceUrl
            ?: return fail(itemId, result.cause.message ?: "Stream download failed")

        runStreamPhase(itemId, resolvedItem, directory, sourceUrl)
    }

    private suspend fun advancePastStreamComplete(
        itemId: String,
        item: DownloadItem,
        directory: UniFile,
    ) {
        // deleteChunks, not resetChunks: the video finished, but streamBytesDownloaded/
        // streamTotalBytes still need to keep reporting the final video size through subtitle
        // fetching and on into COMPLETED — resetChunks would zero them right back out.
        mediaDownloadRepository.deleteChunks(itemId)
        runSubtitlePhase(itemId, item, directory)
    }

    /**
     * Downloads every valid cached subtitle for [item]'s media/episode into the `subtitles/`
     * folder next to the video, disambiguated by [com.flixclusive.core.database.entity.provider.CachedSubtitle.label].
     * A single subtitle's failure is non-fatal — the item still reaches [DownloadItemState.COMPLETED],
     * with [DownloadItem.downloadedSubtitlesCount] reporting what actually arrived.
     */
    private suspend fun runSubtitlePhase(
        itemId: String,
        item: DownloadItem,
        directory: UniFile,
    ) {
        val subtitles = mediaLinksRepository
            .getLinks(
                ownerId = item.ownerId,
                mediaId = item.mediaId,
                episodeNumber = item.episodeNumber,
                seasonNumber = item.seasonNumber,
            ).flatMap { it.subtitles }
            .filter { it.isValid }

        if (subtitles.isEmpty()) {
            mediaDownloadRepository.updateState(itemId, DownloadItemState.COMPLETED, null)
            return
        }

        mediaDownloadRepository.updateState(itemId, DownloadItemState.FETCHING_SUBTITLES, DownloadPhase.SUBTITLES)
        if (item.totalSubtitlesCount == 0) {
            mediaDownloadRepository.setTotalSubtitlesCount(itemId, subtitles.size)
        }

        val subtitlesDirectory = downloadDirectoryRepository.getOrCreateSubtitlesDirectory(directory)
        if (subtitlesDirectory == null) {
            mediaDownloadRepository.updateState(itemId, DownloadItemState.COMPLETED, null)
            return
        }

        val alreadyDownloaded = mediaDownloadRepository.getItem(itemId)?.downloadedSubtitlesCount ?: 0
        for (subtitle in subtitles.drop(alreadyDownloaded)) {
            // Each subtitle file is its own runTransfer() call, and runTransfer() clears any
            // pending interrupt flag the instant it starts — so a pause/stop requested between
            // two (typically near-instant) subtitle transfers would otherwise be silently wiped
            // before shouldInterrupt is ever checked for the next file. Catch it here instead.
            val pendingInterrupt = mediaDownloadRepository.consumeInterruptReason(itemId)
            if (pendingInterrupt != null) {
                return applyInterrupt(itemId, pendingInterrupt, DownloadPhase.SUBTITLES, directory)
            }

            // deleteChunks, not resetChunks — same reason as advancePastStreamComplete: only the
            // stale chunk bookkeeping from the previous subtitle should be cleared here, not the
            // video's already-final streamBytesDownloaded/streamTotalBytes.
            mediaDownloadRepository.deleteChunks(itemId)

            val fileName = DownloadPathUtil.buildSubtitleFileName(
                "${DownloadPathUtil.buildFileTitle(item.mediaTitle)} (${subtitle.label})",
                DownloadPathUtil.extensionFromUrl(
                    subtitle.url,
                    DownloadPathUtil.DEFAULT_SUBTITLE_EXTENSION,
                    DownloadPathUtil.SUBTITLE_EXTENSIONS
                ),
            )
            val destinationFile = downloadDirectoryRepository.getOrCreateFile(subtitlesDirectory, fileName)
                ?: continue

            val result = mediaDownloadRepository.runTransfer(
                id = itemId,
                phase = DownloadPhase.SUBTITLES,
                url = subtitle.url,
                headers = subtitle.customHeaders ?: emptyMap(),
                destinationFile = destinationFile,
                totalBytes = null,
            )

            when (result) {
                is MediaTransferResult.Completed -> mediaDownloadRepository.incrementDownloadedSubtitlesCount(itemId)
                is MediaTransferResult.Cancelled -> return handleInterrupted(itemId, DownloadPhase.SUBTITLES, directory)
                is MediaTransferResult.Failed -> Unit
            }
        }

        mediaDownloadRepository.updateState(itemId, DownloadItemState.COMPLETED, null)
    }

    private suspend fun handleInterrupted(
        itemId: String,
        phase: DownloadPhase,
        directory: UniFile,
    ) {
        val reason = mediaDownloadRepository.consumeInterruptReason(itemId) ?: DownloadInterruptReason.PAUSE
        applyInterrupt(itemId, reason, phase, directory)
    }

    private suspend fun applyInterrupt(
        itemId: String,
        reason: DownloadInterruptReason,
        phase: DownloadPhase,
        directory: UniFile,
    ) {
        when (reason) {
            DownloadInterruptReason.STOP -> {
                directory.delete()
                mediaDownloadRepository.resetChunks(itemId)
                mediaDownloadRepository.updateState(itemId, DownloadItemState.STOPPED, null)
            }
            DownloadInterruptReason.PAUSE -> mediaDownloadRepository.updateState(
                itemId,
                DownloadItemState.PAUSED,
                phase
            )
        }
    }

    private suspend fun fail(
        itemId: String,
        message: String,
    ) {
        mediaDownloadRepository.markError(itemId, message)
        mediaDownloadRepository.updateState(itemId, DownloadItemState.FAILED, null)
    }

    private suspend fun resolveDirectory(item: DownloadItem): UniFile? =
        getDownloadDirectoryUseCase(item.mediaId, item.mediaTitle, item.seasonNumber, item.episodeNumber)

    companion object {
        // Catches a "successful" transfer that actually saved an error page or empty response
        // (e.g. a dead link the initial probe didn't catch) instead of a real video file.
        private const val MIN_VALID_STREAM_FILE_BYTES = 100 * 1024L
    }
}
