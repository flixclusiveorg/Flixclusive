package com.flixclusive.data.downloads.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.data.downloads.model.DownloadStatus
import com.flixclusive.data.downloads.repository.DownloadRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds
import com.flixclusive.core.strings.R as StringR

@AndroidEntryPoint
class DownloadService : Service() {
    @Inject
    lateinit var downloadRepository: DownloadRepository

    @Inject
    lateinit var appDispatchers: AppDispatchers

    private lateinit var wakeLock: PowerManager.WakeLock
    internal val activeDownloads = mutableMapOf<String, Job>() // Internal for testing

    private val lastUpdateTimes = ConcurrentHashMap<Int, Long>()
    private val notificationThrottleMs = 1000L

    // Do not cancel this scope directly - use stopServiceJob to manage service stopping logic
    private val serviceScope by lazy { CoroutineScope(appDispatchers.io + SupervisorJob()) }
    private var stopServiceJob: Job? = null

    /**
     * Binder for testing purposes - allows tests to access service internals
     */
    inner class DownloadServiceBinder : Binder() {
        val service: DownloadService
            get() = this@DownloadService

        val activeDownloadCount: Int
            get() = activeDownloads.size

        val isWakeLockHeld: Boolean
            get() = ::wakeLock.isInitialized && wakeLock.isHeld
    }

    companion object {
        internal const val ACTION_START_DOWNLOAD = "START_DOWNLOAD"
        internal const val ACTION_CANCEL_DOWNLOAD = "CANCEL_DOWNLOAD"
        internal const val EXTRA_DOWNLOAD_ID = "download_id"
        internal const val EXTRA_URL = "url"
        internal const val EXTRA_FILE_PATH = "file_path"
        internal const val EXTRA_FILE_NAME = "file_name"

        internal const val NOTIFICATION_CHANNEL_ID = "download_channel"

        fun startDownload(
            context: Context,
            downloadId: String,
            url: String,
            filePath: String,
            fileName: String,
        ) {
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_START_DOWNLOAD
                putExtra(EXTRA_DOWNLOAD_ID, downloadId)
                putExtra(EXTRA_URL, url)
                putExtra(EXTRA_FILE_PATH, filePath)
                putExtra(EXTRA_FILE_NAME, fileName)
            }

            ContextCompat.startForegroundService(context, intent)
        }

        fun cancelDownload(
            context: Context,
            downloadId: String,
        ) {
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_CANCEL_DOWNLOAD
                putExtra(EXTRA_DOWNLOAD_ID, downloadId)
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()

        // Create notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }

        setupWakeLock()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        when (intent?.action) {
            ACTION_START_DOWNLOAD -> {
                val downloadId = intent.getStringExtra(EXTRA_DOWNLOAD_ID) ?: return START_NOT_STICKY
                val url = intent.getStringExtra(EXTRA_URL) ?: return START_NOT_STICKY
                val filePath = intent.getStringExtra(EXTRA_FILE_PATH) ?: return START_NOT_STICKY
                val fileName = intent.getStringExtra(EXTRA_FILE_NAME) ?: return START_NOT_STICKY

                startDownload(downloadId, url, filePath, fileName)
            }

            ACTION_CANCEL_DOWNLOAD -> {
                val downloadId = intent.getStringExtra(EXTRA_DOWNLOAD_ID) ?: return START_NOT_STICKY
                cancelDownload(downloadId)
            }
        }

        return START_STICKY
    }

    /**
     * Sets up a partial wake lock to keep the CPU running during downloads.
     * **This ensures that downloads continue even if the device goes to sleep.**
     * */
    private fun setupWakeLock() {
        val powerManager: PowerManager = getSystemService()!!
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "${javaClass.name}:DownloadWakeLock",
        )

        // We set a 30 minutes timeout to avoid holding the wake lock indefinitely
        // in case something goes wrong. Downloads should typically not take this long.
        val timeout = 30 * 60 * 1000L
        wakeLock.acquire(timeout)
        this.wakeLock = wakeLock
    }

    private fun releaseWakeLockIfNeeded() {
        if (activeDownloads.isEmpty() && ::wakeLock.isInitialized && wakeLock.isHeld) {
            wakeLock.release()
        }
    }

    private fun startDownload(
        downloadId: String,
        url: String,
        filePath: String,
        fileName: String,
    ) {
        // If the download is already active, ignore the request
        if (activeDownloads[downloadId]?.isActive == true) return

        stopServiceJob?.cancel()

        val notificationId = downloadId.hashCode()
        safeStartForeground(notificationId, createNotification(fileName, downloadId))

        val job = serviceScope.launch {
            try {
                val file = File(filePath, fileName)

                downloadRepository.executeDownload(downloadId, url, file)
                downloadRepository.getDownloadState(downloadId).collect { state ->
                    updateNotification(notificationId, fileName, downloadId, state.progress, state.status)

                    if (state.status.isFinished) {
                        activeDownloads.remove(downloadId)
                    }

                    if (activeDownloads.isEmpty()) {
                        scheduleServiceStop()
                    }
                }
            } catch (e: Throwable) {
                e.printStackTrace()
                activeDownloads.remove(downloadId)
                if (activeDownloads.isEmpty()) {
                    stopForeground()
                    releaseWakeLockIfNeeded()
                    stopSelf()
                }
            }
        }

        activeDownloads[downloadId] = job
    }

    private fun cancelDownload(downloadId: String) {
        activeDownloads[downloadId]?.cancel()
        downloadRepository.cancelDownload(downloadId)
        activeDownloads.remove(downloadId)

        val notificationId = downloadId.hashCode()
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(notificationId)

        if (activeDownloads.isEmpty()) {
            stopForeground()
            releaseWakeLockIfNeeded()
            stopSelf()
        }
    }

    private fun scheduleServiceStop(delayMs: Long = 5000L) {
        stopServiceJob?.cancel() // Cancel any existing stop job
        stopServiceJob = serviceScope.launch {
            delay(delayMs.milliseconds) // Wait for 5 seconds before stopping the service
            if (activeDownloads.isEmpty()) {
                stopForeground()
                releaseWakeLockIfNeeded()
                stopSelf()
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun stopForeground() {
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

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification(fileName: String, downloadId: String): Notification {
        val cancelIntent = Intent(this, DownloadService::class.java).apply {
            action = ACTION_CANCEL_DOWNLOAD
            putExtra(EXTRA_DOWNLOAD_ID, downloadId)
        }
        val cancelPendingIntent = PendingIntent.getService(
            this,
            downloadId.hashCode(),
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat
            .Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Downloading $fileName")
            .setContentText("Starting...")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, 0, false)
            .setOngoing(true)
            .setContentIntent(createContentIntent())
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                getString(StringR.string.label_cancel),
                cancelPendingIntent,
            ).build()
    }

    private fun createContentIntent(): PendingIntent {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun updateNotification(
        notificationId: Int,
        fileName: String,
        downloadId: String,
        progress: Float,
        status: DownloadStatus,
    ) {
        val currentTime = System.currentTimeMillis()
        val lastUpdateTime = lastUpdateTimes[notificationId] ?: 0L

        if (
            status.isFinished ||
            progress == 0f ||
            progress == 100f ||
            currentTime - lastUpdateTime > notificationThrottleMs
        ) {
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

            val notificationBuilder = NotificationCompat
                .Builder(this, NOTIFICATION_CHANNEL_ID)
                .setContentIntent(createContentIntent())

            val notification = when (status) {
                DownloadStatus.DOWNLOADING -> {
                    val cancelIntent = Intent(this, DownloadService::class.java).apply {
                        action = ACTION_CANCEL_DOWNLOAD
                        putExtra(EXTRA_DOWNLOAD_ID, downloadId)
                    }
                    val cancelPendingIntent = PendingIntent.getService(
                        this,
                        downloadId.hashCode(),
                        cancelIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                    )

                    notificationBuilder
                        .setContentTitle("Downloading ${fileName.substringBeforeLast(".")}")
                        .setContentText("$progress%")
                        .setSmallIcon(android.R.drawable.stat_sys_download)
                        .setProgress(100, progress.toInt(), false)
                        .setOngoing(true)
                        .addAction(
                            android.R.drawable.ic_menu_close_clear_cancel,
                            getString(StringR.string.label_cancel),
                            cancelPendingIntent,
                        ).build()
                }

                DownloadStatus.COMPLETED -> {
                    notificationBuilder
                        .setContentTitle("Download completed")
                        .setContentText(fileName.substringBeforeLast("."))
                        .setSmallIcon(android.R.drawable.stat_sys_download_done)
                        .setProgress(0, 0, false)
                        .setOngoing(false)
                        .build()
                }

                DownloadStatus.FAILED -> {
                    notificationBuilder
                        .setContentTitle("Download failed")
                        .setContentText(fileName.substringBeforeLast("."))
                        .setSmallIcon(android.R.drawable.stat_notify_error)
                        .setProgress(0, 0, false)
                        .setOngoing(false)
                        .build()
                }

                else -> {
                    return
                }
            }

            notificationManager.notify(notificationId, notification)
            lastUpdateTimes[notificationId] = currentTime
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        return DownloadServiceBinder()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopServiceJob?.cancel()
        activeDownloads.forEach { (_, job) -> job.cancel() }

        if (::wakeLock.isInitialized && wakeLock.isHeld) {
            wakeLock.release()
        }
    }
}
