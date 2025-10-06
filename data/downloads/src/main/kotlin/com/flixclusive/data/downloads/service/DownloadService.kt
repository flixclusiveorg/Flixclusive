package com.flixclusive.data.downloads.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
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
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class DownloadService : Service() {
    @Inject
    lateinit var downloadRepository: DownloadRepository

    @Inject
    lateinit var appDispatchers: AppDispatchers

    private lateinit var wakeLock: PowerManager.WakeLock
    internal val activeDownloads = mutableMapOf<String, Job>() // Internal for testing
    private val serviceScope by lazy { CoroutineScope(appDispatchers.io + SupervisorJob()) }

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
        internal const val NOTIFICATION_ID = 1001

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

        startForeground(NOTIFICATION_ID, createNotification(fileName))

        val job = serviceScope.launch {
            try {
                val file = File(filePath, fileName)

                downloadRepository.executeDownload(downloadId, url, file)
                downloadRepository.getDownloadState(downloadId).takeWhile { state ->
                    updateNotification(fileName, state.progress, state.status)

                    if (state.status.isFinished) {
                        activeDownloads.remove(downloadId)

                        if (activeDownloads.isEmpty()) {
                            stopForeground()
                            releaseWakeLockIfNeeded()
                            stopSelf()
                        }
                    }

                    !state.status.isFinished
                }.collect()
            } catch (e: Exception) {
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

        if (activeDownloads.isEmpty()) {
            stopForeground()
            releaseWakeLockIfNeeded()
            stopSelf()
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

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "Download Service",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Shows download progress"
        }

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification(fileName: String): Notification {
        return NotificationCompat
            .Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Downloading $fileName")
            .setContentText("Starting...")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, 0, false)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(
        fileName: String,
        progress: Int,
        status: DownloadStatus,
    ) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = when (status) {
            DownloadStatus.DOWNLOADING -> {
                NotificationCompat
                    .Builder(this, NOTIFICATION_CHANNEL_ID)
                    .setContentTitle("Downloading $fileName...")
                    .setContentText("$progress%")
                    .setSmallIcon(android.R.drawable.stat_sys_download)
                    .setProgress(100, progress, false)
                    .setOngoing(true)
                    .build()
            }

            DownloadStatus.COMPLETED -> {
                NotificationCompat
                    .Builder(this, NOTIFICATION_CHANNEL_ID)
                    .setContentTitle("Download completed")
                    .setContentText(fileName)
                    .setSmallIcon(android.R.drawable.stat_sys_download_done)
                    .setProgress(0, 0, false)
                    .setOngoing(false)
                    .build()
            }

            DownloadStatus.FAILED -> {
                NotificationCompat
                    .Builder(this, NOTIFICATION_CHANNEL_ID)
                    .setContentTitle("Download failed")
                    .setContentText(fileName)
                    .setSmallIcon(android.R.drawable.stat_notify_error)
                    .setProgress(0, 0, false)
                    .setOngoing(false)
                    .build()
            }

            else -> return
        }

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    override fun onBind(intent: Intent?): IBinder {
        return DownloadServiceBinder()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()

        // Clean up wake lock on service destruction
        if (::wakeLock.isInitialized && wakeLock.isHeld) {
            wakeLock.release()
        }
    }
}
