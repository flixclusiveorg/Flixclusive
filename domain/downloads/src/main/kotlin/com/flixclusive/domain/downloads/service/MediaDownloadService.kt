package com.flixclusive.domain.downloads.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.text.format.Formatter
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
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
import com.flixclusive.core.strings.R as LocaleR

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

    companion object {
        private const val ACTION_PAUSE = "MEDIA_DOWNLOAD_PAUSE"
        private const val ACTION_RESUME = "MEDIA_DOWNLOAD_RESUME"
        private const val ACTION_STOP = "MEDIA_DOWNLOAD_STOP"
        private const val ACTION_RETRY = "MEDIA_DOWNLOAD_RETRY"
        private const val EXTRA_ITEM_ID = "item_id"

        internal const val NOTIFICATION_CHANNEL_ID = "download_channel"
        private const val SUMMARY_NOTIFICATION_ID = Int.MAX_VALUE
        private const val QUEUED_GROUP_NOTIFICATION_ID = Int.MAX_VALUE - 1
        private const val NOTIFICATION_GROUP_KEY = "media_downloads_group"

        fun ensureStarted(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, MediaDownloadService::class.java))
        }
    }

    override fun onCreate() {
        super.onCreate()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }

        // Must run synchronously here: `startForegroundService()` requires `startForeground()`
        // to be called within a few seconds or the OS kills the process with a
        // RemoteServiceException. The real summary notification below is posted once the item
        // Flow in startObservingActiveItems() emits, but that's async and may resolve to zero
        // active items, which previously left startForeground() uncalled entirely.
        safeStartForeground(SUMMARY_NOTIFICATION_ID, buildSummaryNotification(0))

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
                safeStartForeground(SUMMARY_NOTIFICATION_ID, buildSummaryNotification(activeItems.size))

                // QUEUED items are collapsed into a single count notification instead of one each —
                // otherwise a big batch queue floods the shade with rows that have nothing to show
                // yet, and (since QUEUED shares the same download icon as an active transfer) reads
                // as though everything queued is already downloading.
                val (queuedItems, inProgressItems) = activeItems.partition { it.state == DownloadItemState.QUEUED }

                inProgressItems.forEach { item ->
                    notificationManager.notify(item.notificationId(), buildItemNotification(item))
                }

                if (queuedItems.isNotEmpty()) {
                    queuedItems.forEach { notificationManager.cancel(it.notificationId()) }
                    notificationManager.notify(
                        QUEUED_GROUP_NOTIFICATION_ID,
                        buildQueuedGroupNotification(queuedItems.size)
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
        removedIds.forEach { id -> notificationManager.cancel(id.hashCode()) }
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
                notificationManager.cancel(item.notificationId())
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
                notificationManager.notify(item.notificationId(), buildErrorNotification(item))
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
                notificationManager.notify(item.notificationId(), buildCompletedNotification(item))
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

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "Download Service",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Shows download progress"
        }

        val notificationManager: NotificationManager = getSystemService()!!
        notificationManager.createNotificationChannel(channel)
    }

    private fun buildSummaryNotification(activeCount: Int): Notification =
        NotificationCompat
            .Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Downloading")
            .setContentText(
                when {
                    activeCount <= 0 -> "Preparing downloads…"
                    activeCount == 1 -> "1 download in progress"
                    else -> "$activeCount downloads in progress"
                },
            ).setSmallIcon(android.R.drawable.stat_sys_download)
            .setGroup(NOTIFICATION_GROUP_KEY)
            .setGroupSummary(true)
            .setOngoing(true)
            .build()

    private fun buildItemNotification(item: DownloadItem): Notification {
        val (progress, statusText) = progressAndStatusFor(item)

        val icon = if (item.state == DownloadItemState.PAUSED) {
            android.R.drawable.ic_media_pause
        } else {
            android.R.drawable.stat_sys_download
        }

        val builder = NotificationCompat
            .Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(item.displayTitle())
            .setContentText(statusText)
            .setSmallIcon(icon)
            .setGroup(NOTIFICATION_GROUP_KEY)
            .setOngoing(true)

        if (progress != null) {
            builder.setProgress(100, progress, false)
        }

        when (item.state) {
            DownloadItemState.DOWNLOADING_STREAM, DownloadItemState.FETCHING_SUBTITLES ->
                builder.addAction(0, "Pause", actionPendingIntent(ACTION_PAUSE, item.id))
            DownloadItemState.PAUSED ->
                builder.addAction(0, "Resume", actionPendingIntent(ACTION_RESUME, item.id))
            else -> Unit
        }

        if (item.state != DownloadItemState.STOPPED) {
            builder.addAction(0, "Stop", actionPendingIntent(ACTION_STOP, item.id))
        }

        return builder.build()
    }

    private fun buildErrorNotification(item: DownloadItem): Notification {
        val message = item.errorMessage?.takeIf { it.isNotBlank() } ?: "Download failed"

        return NotificationCompat
            .Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(item.displayTitle())
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setGroup(NOTIFICATION_GROUP_KEY)
            .setOngoing(false)
            .setAutoCancel(true)
            .addAction(0, "Retry", actionPendingIntent(ACTION_RETRY, item.id))
            .build()
    }

    private fun buildQueuedGroupNotification(count: Int): Notification =
        NotificationCompat
            .Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Queued")
            .setContentText(if (count == 1) "1 download queued" else "$count downloads queued")
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setGroup(NOTIFICATION_GROUP_KEY)
            .setOngoing(true)
            .build()

    private fun buildCompletedNotification(item: DownloadItem): Notification =
        NotificationCompat
            .Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(item.displayTitle())
            .setContentText("Download complete")
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setGroup(NOTIFICATION_GROUP_KEY)
            .setOngoing(false)
            .setAutoCancel(true)
            .build()

    /** Falls back to the media title, with the `SxxExx` tag appended for episodes — downloads
     * don't persist an episode title of their own (see [DownloadItem]). */
    private fun DownloadItem.displayTitle(): String {
        val season = seasonNumber
        val episode = episodeNumber
        if (season == null || episode == null) return mediaTitle

        val tag = "S${season.toString().padStart(2, '0')}E${episode.toString().padStart(2, '0')}"
        return "$mediaTitle • $tag"
    }

    private fun DownloadItem.notificationId(): Int = id.hashCode()

    private fun progressAndStatusFor(item: DownloadItem): Pair<Int?, String> =
        when (item.state) {
            DownloadItemState.DOWNLOADING_STREAM -> {
                val percent = percentOf(item.streamBytesDownloaded, item.streamTotalBytes)
                val speedSuffix = formatSpeedSuffix(item, isSegments = item.isHlsStream)
                val text = when {
                    // HLS reuses these same columns for a segment count, not a byte count — running
                    // it through formatSize() would render a handful of segments as a bogus "1 KB".
                    item.isHlsStream && item.streamTotalBytes > 0 ->
                        "HLS: ${item.streamBytesDownloaded}/${item.streamTotalBytes} segments$speedSuffix"
                    item.streamTotalBytes > 0 ->
                        "${formatSize(
                            item.streamBytesDownloaded
                        )} / ${formatSize(item.streamTotalBytes)}$speedSuffix"
                    else -> formatSize(item.streamBytesDownloaded) + speedSuffix
                }
                percent to text
            }
            DownloadItemState.FETCHING_SUBTITLES -> {
                val percent = percentOf(item.downloadedSubtitlesCount.toLong(), item.totalSubtitlesCount.toLong())
                // Subtitle transfers are always plain file downloads, regardless of whether the
                // stream itself was HLS — never render this one as "segments/s".
                val speedSuffix = formatSpeedSuffix(item, isSegments = false)
                percent to "${item.downloadedSubtitlesCount}/${item.totalSubtitlesCount}$speedSuffix"
            }
            DownloadItemState.PAUSED -> null to "Paused"
            DownloadItemState.STREAM_COMPLETE -> null to "Preparing subtitles…"
            else -> null to "Queued"
        }

    private fun percentOf(current: Long, total: Long): Int =
        if (total <= 0) 0 else ((current * 100) / total).toInt().coerceIn(0, 100)

    private fun formatSize(bytes: Long): String = Formatter.formatShortFileSize(this, bytes)

    /** `" • 1.2 MB/s"` (or `" • 3 segments/s"` for HLS). Mirrors the download card: the
     * `"Calculating speed…"` placeholder is only for a download that hasn't moved a byte yet, since
     * once something has transferred a zero rate means the transfer has genuinely stalled — and
     * `" • 0 B/s"` says that far more honestly than a placeholder that never resolves. */
    private fun formatSpeedSuffix(item: DownloadItem, isSegments: Boolean): String {
        val hasTransferred = item.streamBytesDownloaded > 0 || item.downloadedSubtitlesCount > 0
        if (item.downloadBytesPerSecond <= 0 && !hasTransferred) {
            return " • ${getString(LocaleR.string.download_progress_speed_calculating)}"
        }

        val speed = if (isSegments) {
            getString(LocaleR.string.download_progress_speed_segments_format, item.downloadBytesPerSecond)
        } else {
            getString(LocaleR.string.download_progress_speed_format, formatSize(item.downloadBytesPerSecond))
        }

        return " • $speed"
    }

    private fun actionPendingIntent(action: String, itemId: String): PendingIntent {
        val intent = Intent(this, MediaDownloadService::class.java).apply {
            this.action = action
            putExtra(EXTRA_ITEM_ID, itemId)
        }
        return PendingIntent.getService(
            this,
            "$action-$itemId".hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
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
