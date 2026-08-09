package com.flixclusive.domain.downloads.controller.impl

import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.database.entity.downloads.DownloadItem
import com.flixclusive.core.database.entity.downloads.DownloadItemState
import com.flixclusive.core.database.entity.downloads.DownloadPhase
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.datastore.model.user.DataPreferences
import com.flixclusive.core.datastore.model.user.UserPreferences
import com.flixclusive.core.datastore.model.user.download.DownloadLinkSortDirection
import com.flixclusive.core.network.monitor.NetworkMonitor
import com.flixclusive.core.util.log.errorLog
import com.flixclusive.data.downloads.model.DownloadInterruptReason
import com.flixclusive.data.downloads.repository.MediaDownloadRepository
import com.flixclusive.domain.downloads.controller.MediaDownloadController
import com.flixclusive.domain.downloads.controller.MediaDownloadServiceController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class MediaDownloadControllerImpl @Inject constructor(
    private val mediaDownloadRepository: MediaDownloadRepository,
    private val runner: MediaDownloadRunner,
    private val networkMonitor: NetworkMonitor,
    private val mediaDownloadServiceController: MediaDownloadServiceController,
    private val dataStoreManager: DataStoreManager,
    private val appDispatchers: AppDispatchers,
) : MediaDownloadController {
    private val scope by lazy { CoroutineScope(appDispatchers.io + SupervisorJob()) }
    private val jobs = ConcurrentHashMap<String, Job>()

    private val dispatchMutex = Mutex()
    private val activeItemIds = mutableSetOf<String>()
    private var resumeSweepJob: Job? = null

    init {
        startDispatchingWhenNetworkAllows()
    }

    /**
     * Re-runs the dispatcher whenever the connection (or the Wi-Fi-only preference) starts allowing
     * downloads again.
     *
     * Without this the gate is one-way: items blocked on mobile data stay queued with nothing
     * watching for Wi-Fi, so they only moved when something else happened to call the dispatcher —
     * in practice the next app launch. Collecting rather than polling also keeps
     * [NetworkMonitor.isMetered]'s shared upstream alive, so its emissions track the real
     * connection instead of stopping between one-shot reads.
     *
     * Failures resubscribe rather than being caught: catching completes the flow, which would
     * retire the gate for the rest of the process and put us right back at the one-way behaviour
     * above. The preferences flow throws transiently before a user session exists, which is
     * precisely when this starts.
     */
    private fun startDispatchingWhenNetworkAllows() {
        scope.launch {
            combine(
                networkMonitor.isMetered,
                dataStoreManager
                    .getUserPrefsAsFlow(UserPreferences.DATA_PREFS_KEY, DataPreferences::class)
                    .map { it.downloadOnWifiOnly }
                    .distinctUntilChanged(),
            ) { metered, wifiOnly -> !wifiOnly || !metered }
                .distinctUntilChanged()
                .retry { cause ->
                    errorLog("Network watch for download dispatch failed, retrying: ${cause.message}")
                    delay(NETWORK_WATCH_RETRY_DELAY_MS)
                    true
                }.filter { it }
                .collect { dispatchNext() }
        }
    }

    /**
     * Registers [block] as [itemId]'s in-flight job, guaranteeing the map entry is dropped when it
     * finishes.
     *
     * Removal has to hang off the job rather than a `finally` inside it: [dispatchOrQueue] returns
     * early when the network disallows downloads or no slot is free, and those paths would otherwise
     * strand a completed Job in a map on a `@Singleton` forever. The two-argument remove is
     * deliberate — [dispatchNext] can install a new job for an id while the previous one is still
     * unwinding, and an unconditional remove would evict the newcomer.
     */
    private fun launchDownload(
        itemId: String,
        block: suspend () -> Unit,
    ) {
        val job = scope.launch { block() }
        jobs[itemId] = job
        job.invokeOnCompletion { jobs.remove(itemId, job) }
    }

    override fun start(itemId: String) {
        if (jobs[itemId]?.isActive == true) return
        launchDownload(itemId) { dispatchOrQueue(itemId) }
    }

    override fun resume(itemId: String) = start(itemId)

    override fun retry(itemId: String) {
        if (jobs[itemId]?.isActive == true) return
        launchDownload(itemId) {
            mediaDownloadRepository.resetChunks(itemId)
            mediaDownloadRepository.updateState(itemId, DownloadItemState.QUEUED, null)
            dispatchOrQueue(itemId)
        }
    }

    override fun resumeInterrupted() {
        // The activity fires this on every STARTED, and the network gate below now waits on a user
        // session rather than failing without one — so before the first sign-in each launch would
        // otherwise park another sweep, and every one of them would run the moment prefs arrive.
        if (resumeSweepJob?.isActive == true) return

        resumeSweepJob = scope.launch {
            // Left as-is rather than requeued when held back: the rows stay in whatever state the
            // dead process left them, and the next sweep on an unmetered connection recovers them.
            if (!isNetworkAllowed()) return@launch

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
                runner.resolveDirectory(item)?.delete()
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

    /**
     * Leaves a download the network gate turned away in the one state the dispatcher picks up from.
     *
     * Resuming a [DownloadItemState.PAUSED] item is the case that needs this: it would otherwise
     * stay paused, and [dispatchNext] only ever pulls queued rows — so the tap would be quietly
     * forgotten even once Wi-Fi came back. Everything else arrives here already queued. The phase is
     * carried over so a resume into the subtitle phase doesn't restart the video.
     */
    private suspend fun parkUntilNetworkAllows(itemId: String) {
        val item = mediaDownloadRepository.getItem(itemId) ?: return
        if (item.state != DownloadItemState.PAUSED) return

        mediaDownloadRepository.updateState(itemId, DownloadItemState.QUEUED, resumePhaseOf(item))
    }

    private suspend fun stopInactiveItem(
        itemId: String,
        item: DownloadItem,
    ) {
        runner.resolveDirectory(item)?.delete()
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
     * Whether any transfer may begin on the connection the device is on right now.
     *
     * Gates every path that starts a download, explicit taps included — the preference promises
     * downloads happen on Wi-Fi, not merely that they resume there. A blocked item is still queued
     * and still listed, so a tap leaves something visible behind rather than appearing to do
     * nothing, and it starts by itself once an unmetered connection is back.
     */
    private suspend fun isNetworkAllowed(): Boolean {
        val wifiOnly = dataStoreManager
            .getUserPrefsAsFlow(UserPreferences.DATA_PREFS_KEY, DataPreferences::class)
            .first()
            .downloadOnWifiOnly

        // isMeteredNow(), not isMetered.first(): the flow is shared with WhileSubscribed, so a
        // one-shot collect replays whatever was last observed rather than reading the connection
        // the device is on at this moment — which let downloads start on mobile data after a
        // switch away from Wi-Fi.
        return !wifiOnly || !networkMonitor.isMeteredNow()
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
        // Covers start/resume/retry.
        if (!isNetworkAllowed()) return parkUntilNetworkAllows(itemId)
        if (!tryReserveSlot(itemId)) return

        mediaDownloadServiceController.ensureRunning()

        try {
            runner.run(itemId)
        } finally {
            releaseSlot(itemId)
            dispatchNext()
        }
    }

    /** Fills every free concurrency slot with the oldest QUEUED items, FIFO, until none remain or the limit is hit. */
    private suspend fun dispatchNext() {
        if (!isNetworkAllowed()) return

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

            launchDownload(next.id) {
                try {
                    runner.run(next.id)
                } finally {
                    releaseSlot(next.id)
                    dispatchNext()
                }
            }
        }
    }

    companion object {
        // Spaces out resubscription so an upstream that rethrows immediately cannot spin.
        private const val NETWORK_WATCH_RETRY_DELAY_MS = 1_000L
    }
}
