package com.flixclusive.domain.downloads.service

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.database.entity.downloads.DownloadItem
import com.flixclusive.core.database.entity.downloads.DownloadItemState
import com.flixclusive.data.downloads.repository.MediaDownloadRepository
import com.flixclusive.domain.downloads.controller.MediaDownloadController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

/**
 * Reuses [com.flixclusive.data.downloads.service.DownloadService]'s notification channel id and
 * throttle/wake-lock/auto-stop pattern; kept as a separate class per Req 2 since the two services
 * model entirely different states (chunked pause/resume/batch vs. single-shot install downloads)
 * and the original's Intent/extras contract has no room for either.
 *
 * Worth knowing before assuming this can run indefinitely: Android 14+ caps `dataSync` foreground
 * services at roughly six hours a day, after which the system stops the service. Nothing here
 * detects that yet — a download session long enough to hit it will simply end, and the startup
 * sweep in [MediaDownloadController.resumeInterrupted] is what picks the pieces back up.
 */
@AndroidEntryPoint
class MediaDownloadService : Service() {
    @Inject
    lateinit var mediaDownloadController: MediaDownloadController

    @Inject
    lateinit var mediaDownloadRepository: MediaDownloadRepository

    @Inject
    lateinit var appDispatchers: AppDispatchers

    private lateinit var wakeLock: PowerManager.WakeLock
    private val serviceScope by lazy { CoroutineScope(appDispatchers.io + SupervisorJob()) }
    private var observerJob: Job? = null
    private var stopServiceJob: Job? = null

    /** Ids of [DownloadItemState.FAILED] items already notified, so re-emissions of the same
     * failure (e.g. from an unrelated item's progress update) don't re-alert. Cleared once the
     * item is retried/deleted and no longer reports as failed. */
    private val notifiedFailureIds = mutableSetOf<String>()

    /** Ids of [DownloadItemState.COMPLETED] items already notified — mirrors [notifiedFailureIds]
     * so a completed item's notification is replaced with a "Complete" message once instead of
     * just vanishing once it drops out of [startObservingActiveItems]'s active-items set. */
    private val notifiedCompletionIds = mutableSetOf<String>()

    /** Every item id seen in the last emission, so a row that disappears entirely (deleted) can
     * have its notification explicitly cancelled — nothing else ever does, since Android doesn't
     * remove a previously-posted notification just because the underlying data went away. */
    private var knownItemIds: Set<String> = emptySet()

    /** Ids of [DownloadItemState.STOPPED] items already dismissed — mirrors [notifiedFailureIds],
     * but cancels the notification outright instead of replacing it, so tapping Stop closes the
     * row immediately rather than leaving it stuck on its last in-progress message. */
    private val dismissedStoppedIds = mutableSetOf<String>()

    private val notifications by lazy { MediaDownloadNotificationFactory(this) }

    companion object {
        internal const val ACTION_PAUSE = "MEDIA_DOWNLOAD_PAUSE"
        internal const val ACTION_RESUME = "MEDIA_DOWNLOAD_RESUME"
        internal const val ACTION_STOP = "MEDIA_DOWNLOAD_STOP"
        internal const val ACTION_RETRY = "MEDIA_DOWNLOAD_RETRY"
        internal const val EXTRA_ITEM_ID = "item_id"

        internal const val NOTIFICATION_CHANNEL_ID = "download_channel"
        private const val SUMMARY_NOTIFICATION_ID = Int.MAX_VALUE
        private const val QUEUED_GROUP_NOTIFICATION_ID = Int.MAX_VALUE - 1

        fun ensureStarted(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, MediaDownloadService::class.java))
        }
    }

    override fun onCreate() {
        super.onCreate()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notifications.createNotificationChannel()
        }

        // Must run synchronously here: `startForegroundService()` requires `startForeground()`
        // to be called within a few seconds or the OS kills the process with a
        // RemoteServiceException. The real summary notification below is posted once the item
        // Flow in startObservingActiveItems() emits, but that's async and may resolve to zero
        // active items, which previously left startForeground() uncalled entirely.
        safeStartForeground(SUMMARY_NOTIFICATION_ID, notifications.buildSummary(0))

        setupWakeLock()
        startObservingActiveItems()

        // Covers the START_STICKY path: the OS revives this service after a process death with a
        // null intent and no activity in sight, so nothing else would ever restart the transfers
        // that death interrupted. Idempotent, and items already being transferred are left alone.
        mediaDownloadController.resumeInterrupted()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val itemId = intent?.getStringExtra(EXTRA_ITEM_ID)

        if (itemId != null) {
            when (intent.action) {
                ACTION_PAUSE -> mediaDownloadController.pause(itemId)
                ACTION_RESUME -> mediaDownloadController.resume(itemId)
                ACTION_STOP -> mediaDownloadController.stop(itemId)
                ACTION_RETRY -> mediaDownloadController.retry(itemId)
            }
        }

        return START_STICKY
    }

    private fun setupWakeLock() {
        val powerManager: PowerManager = getSystemService()!!
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "${javaClass.name}:MediaDownloadWakeLock",
        )
        wakeLock.acquire(WAKE_LOCK_TIMEOUT_MS)
    }

    /**
     * Re-arms the wake lock while work remains. The lock is deliberately taken with a timeout — one
     * without is a battery drain that outlives any bug that strands it — but that meant a download
     * running longer than [WAKE_LOCK_TIMEOUT_MS] silently lost it and the device could sleep
     * mid-transfer. Renewing from the progress observer costs nothing and bounds the damage of a
     * stranded lock to one timeout window.
     */
    private fun renewWakeLock() {
        if (!::wakeLock.isInitialized || wakeLock.isHeld) return
        wakeLock.acquire(WAKE_LOCK_TIMEOUT_MS)
    }

    private fun startObservingActiveItems() {
        observerJob = serviceScope.launch {
            mediaDownloadRepository.observeAllItems().collectLatest { items ->
                val notificationManager: NotificationManager = getSystemService()!!

                cancelRemovedItemNotifications(items, notificationManager)
                cancelStoppedNotifications(items, notificationManager)
                notifyNewFailures(items, notificationManager)
                notifyNewCompletions(items, notificationManager)

                // QUEUED counts as active too: a freshly queued item stays QUEUED for the whole
                // link-resolution/probing window before its state ever reaches DOWNLOADING_STREAM,
                // so excluding it here made the service tear itself down mid-resolution.
                val activeItems = items.filter { !it.state.isTerminal }

                if (activeItems.isEmpty()) {
                    notificationManager.cancel(QUEUED_GROUP_NOTIFICATION_ID)
                    scheduleServiceStop()
                    return@collectLatest
                }

                stopServiceJob?.cancel()
                renewWakeLock()
                safeStartForeground(SUMMARY_NOTIFICATION_ID, notifications.buildSummary(activeItems.size))

                // QUEUED items are collapsed into a single count notification instead of one each —
                // otherwise a big batch queue floods the shade with rows that have nothing to show
                // yet, and (since QUEUED shares the same download icon as an active transfer) reads
                // as though everything queued is already downloading.
                val (queuedItems, inProgressItems) = activeItems.partition { it.state == DownloadItemState.QUEUED }

                inProgressItems.forEach { item ->
                    notificationManager.notify(notifications.notificationId(item), notifications.buildItem(item))
                }

                if (queuedItems.isNotEmpty()) {
                    queuedItems.forEach { notificationManager.cancel(notifications.notificationId(it)) }
                    notificationManager.notify(
                        QUEUED_GROUP_NOTIFICATION_ID,
                        notifications.buildQueuedGroup(queuedItems.size)
                    )
                } else {
                    notificationManager.cancel(QUEUED_GROUP_NOTIFICATION_ID)
                }
            }
        }
    }

    private fun cancelRemovedItemNotifications(items: List<DownloadItem>, notificationManager: NotificationManager) {
        val currentIds = items.map { it.id }.toSet()
        val removedIds = knownItemIds - currentIds
        removedIds.forEach { id ->
            // Removed from the map as well as cancelled: the item is gone for good, so holding its
            // notification id would leak an entry per deleted download for the service's lifetime.
            notifications.forget(id)?.let(notificationManager::cancel)
        }
        knownItemIds = currentIds
    }

    /** Dismisses a [DownloadItemState.STOPPED] item's notification the first time it's seen —
     * stopped items are terminal, so [startObservingActiveItems] otherwise never touches their
     * notification again, leaving Stop's last "Downloading…"/"Paused" message stuck forever. */
    private fun cancelStoppedNotifications(items: List<DownloadItem>, notificationManager: NotificationManager) {
        val stoppedItems = items.filter { it.state == DownloadItemState.STOPPED }
        dismissedStoppedIds.retainAll(stoppedItems.map { it.id }.toSet())

        stoppedItems.forEach { item ->
            if (dismissedStoppedIds.add(item.id)) {
                notificationManager.cancel(notifications.notificationId(item))
            }
        }
    }

    /** Posts a dismissible error notification the first time an item is seen as [DownloadItemState.FAILED] —
     * failed items are terminal, so [startObservingActiveItems] otherwise never surfaces them. */
    private fun notifyNewFailures(items: List<DownloadItem>, notificationManager: NotificationManager) {
        val failedItems = items.filter { it.state == DownloadItemState.FAILED }
        notifiedFailureIds.retainAll(failedItems.map { it.id }.toSet())

        failedItems.forEach { item ->
            if (notifiedFailureIds.add(item.id)) {
                notificationManager.notify(notifications.notificationId(item), notifications.buildError(item))
            }
        }
    }

    /** Posts a dismissible "Complete" notification the first time an item is seen as
     * [DownloadItemState.COMPLETED] — completed items are terminal, so [startObservingActiveItems]
     * otherwise never surfaces them again, and the item's last in-progress notification would
     * otherwise sit there stale (or disappear entirely once the service stops itself) instead of
     * reflecting that the download finished. */
    private fun notifyNewCompletions(items: List<DownloadItem>, notificationManager: NotificationManager) {
        val completedItems = items.filter { it.state == DownloadItemState.COMPLETED }
        notifiedCompletionIds.retainAll(completedItems.map { it.id }.toSet())

        completedItems.forEach { item ->
            if (notifiedCompletionIds.add(item.id)) {
                notificationManager.notify(notifications.notificationId(item), notifications.buildCompleted(item))
            }
        }
    }

    private fun scheduleServiceStop(delayMs: Long = SERVICE_STOP_DELAY_MS) {
        stopServiceJob?.cancel()
        stopServiceJob = serviceScope.launch {
            delay(delayMs.milliseconds)
            stopForegroundAndSelf()
        }
    }

    private fun stopForegroundAndSelf() {
        stopForegroundCompat()
        if (::wakeLock.isInitialized && wakeLock.isHeld) {
            wakeLock.release()
        }
        stopSelf()
    }

    @Suppress("DEPRECATION")
    private fun stopForegroundCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            stopForeground(true)
        }
    }

    private fun safeStartForeground(id: Int, notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(id, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(id, notification)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        observerJob?.cancel()
        stopServiceJob?.cancel()

        if (::wakeLock.isInitialized && wakeLock.isHeld) {
            wakeLock.release()
        }
    }
}

/** Renewed from the progress observer for as long as work remains — see `renewWakeLock`. Downloads
 * routinely outlast this on their own. */
private const val WAKE_LOCK_TIMEOUT_MS = 30 * 60 * 1000L
private const val SERVICE_STOP_DELAY_MS = 5000L
