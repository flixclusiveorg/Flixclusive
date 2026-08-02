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

        fun pause(context: Context, itemId: Long) = sendAction(context, ACTION_PAUSE, itemId)

        fun resume(context: Context, itemId: Long) = sendAction(context, ACTION_RESUME, itemId)

        fun stop(context: Context, itemId: Long) = sendAction(context, ACTION_STOP, itemId)

        fun retry(context: Context, itemId: Long) = sendAction(context, ACTION_RETRY, itemId)

        private fun sendAction(context: Context, action: String, itemId: Long) {
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

        setupWakeLock()
        startObservingActiveItems()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val itemId = intent?.getLongExtra(EXTRA_ITEM_ID, -1)?.takeIf { it >= 0 }

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
                val activeItems = items.filter { !it.state.isTerminal && it.state != DownloadItemState.QUEUED }

                if (activeItems.isEmpty()) {
                    scheduleServiceStop()
                    return@collectLatest
                }

                stopServiceJob?.cancel()
                safeStartForeground(SUMMARY_NOTIFICATION_ID, buildSummaryNotification(activeItems.size))

                val notificationManager: NotificationManager = getSystemService()!!
                activeItems.forEach { item ->
                    notificationManager.notify(item.id.toInt(), buildItemNotification(item))
                }
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
            .setContentText(if (activeCount == 1) "1 download in progress" else "$activeCount downloads in progress")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setGroup(NOTIFICATION_GROUP_KEY)
            .setGroupSummary(true)
            .setOngoing(true)
            .build()

    private fun buildItemNotification(item: DownloadItem): Notification {
        val title = item.episodeTitle?.takeIf { it.isNotBlank() } ?: item.mediaTitle
        val (progress, statusText) = progressAndStatusFor(item)

        val builder = NotificationCompat
            .Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
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

    private fun progressAndStatusFor(item: DownloadItem): Pair<Int?, String> =
        when (item.state) {
            DownloadItemState.DOWNLOADING_STREAM -> percentOf(item.streamBytesDownloaded, item.streamTotalBytes)
                .let { it to "Downloading video… $it%" }
            DownloadItemState.FETCHING_SUBTITLES -> percentOf(item.subtitleBytesDownloaded, item.subtitleTotalBytes)
                .let { it to "Downloading subtitles… $it%" }
            DownloadItemState.PAUSED -> null to "Paused"
            DownloadItemState.STREAM_COMPLETE -> null to "Preparing subtitles…"
            else -> null to "Queued"
        }

    private fun percentOf(bytesDownloaded: Long, totalBytes: Long): Int =
        if (totalBytes <= 0) 0 else ((bytesDownloaded * 100) / totalBytes).toInt().coerceIn(0, 100)

    private fun actionPendingIntent(action: String, itemId: Long): PendingIntent {
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
