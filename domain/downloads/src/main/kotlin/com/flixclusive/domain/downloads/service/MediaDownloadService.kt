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

/**
 * Reuses [com.flixclusive.data.downloads.service.DownloadService]'s notification channel id and
 * throttle/wake-lock/auto-stop pattern; kept as a separate class per Req 2 since the two services
 * model entirely different states (chunked pause/resume/batch vs. single-shot install downloads)
 * and the original's Intent/extras contract has no room for either.
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

    companion object {
        private const val ACTION_PAUSE = "MEDIA_DOWNLOAD_PAUSE"
        private const val ACTION_RESUME = "MEDIA_DOWNLOAD_RESUME"
        private const val ACTION_STOP = "MEDIA_DOWNLOAD_STOP"
        private const val ACTION_RETRY = "MEDIA_DOWNLOAD_RETRY"
        private const val EXTRA_ITEM_ID = "item_id"

        internal const val NOTIFICATION_CHANNEL_ID = "download_channel"
        private const val SUMMARY_NOTIFICATION_ID = Int.MAX_VALUE
        private const val NOTIFICATION_GROUP_KEY = "media_downloads_group"

        fun ensureStarted(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, MediaDownloadService::class.java))
        }

        fun pause(context: Context, itemId: String) = sendAction(context, ACTION_PAUSE, itemId)

        fun resume(context: Context, itemId: String) = sendAction(context, ACTION_RESUME, itemId)

        fun stop(context: Context, itemId: String) = sendAction(context, ACTION_STOP, itemId)

        fun retry(context: Context, itemId: String) = sendAction(context, ACTION_RETRY, itemId)

        private fun sendAction(context: Context, action: String, itemId: String) {
            val intent = Intent(context, MediaDownloadService::class.java).apply {
                this.action = action
                putExtra(EXTRA_ITEM_ID, itemId)
            }
            ContextCompat.startForegroundService(context, intent)
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

    private fun startObservingActiveItems() {
        observerJob = serviceScope.launch {
            mediaDownloadRepository.observeAllItems().collectLatest { items ->
                // QUEUED counts as active too: a freshly queued item stays QUEUED for the whole
                // link-resolution/probing window before its state ever reaches DOWNLOADING_STREAM,
                // so excluding it here made the service tear itself down mid-resolution.
                val activeItems = items.filter { !it.state.isTerminal }
                val notificationManager: NotificationManager = getSystemService()!!

                notifyNewFailures(items, notificationManager)

                if (activeItems.isEmpty()) {
                    scheduleServiceStop()
                    return@collectLatest
                }

                stopServiceJob?.cancel()
                safeStartForeground(SUMMARY_NOTIFICATION_ID, buildSummaryNotification(activeItems.size))

                activeItems.forEach { item ->
                    notificationManager.notify(item.notificationId(), buildItemNotification(item))
                }
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

        val builder = NotificationCompat
            .Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(item.displayTitle())
            .setContentText(statusText)
            .setSmallIcon(android.R.drawable.stat_sys_download)
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
            DownloadItemState.DOWNLOADING_STREAM -> percentOf(item.streamBytesDownloaded, item.streamTotalBytes)
                .let { it to "Downloading video… $it%" }
            DownloadItemState.FETCHING_SUBTITLES ->
                percentOf(item.downloadedSubtitlesCount.toLong(), item.totalSubtitlesCount.toLong())
                    .let { it to "Downloading subtitles… ${item.downloadedSubtitlesCount}/${item.totalSubtitlesCount}" }
            DownloadItemState.PAUSED -> null to "Paused"
            DownloadItemState.STREAM_COMPLETE -> null to "Preparing subtitles…"
            else -> null to "Queued"
        }

    private fun percentOf(current: Long, total: Long): Int =
        if (total <= 0) 0 else ((current * 100) / total).toInt().coerceIn(0, 100)

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

private const val WAKE_LOCK_TIMEOUT_MS = 30 * 60 * 1000L
private const val SERVICE_STOP_DELAY_MS = 5000L
