package com.flixclusive.service.app_updater

import android.app.ActivityManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.flixclusive.R
import com.flixclusive.common.LoggerUtils.errorLog
import com.flixclusive.di.IoDispatcher
import com.flixclusive.presentation.mobile.screens.update.UPDATE_LOCATION
import com.flixclusive.presentation.mobile.screens.update.UPDATE_PROGRESS
import com.flixclusive.presentation.mobile.screens.update.UPDATE_PROGRESS_RECEIVER_ACTION
import com.flixclusive.service.app_updater.utils.ProgressListener
import com.flixclusive.service.app_updater.utils.await
import com.flixclusive.service.app_updater.utils.newCachelessCallWithProgress
import com.flixclusive.service.app_updater.utils.saveTo
import com.flixclusive.service.utils.createChannel
import com.flixclusive.service.utils.getUriCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.internal.http2.ErrorCode
import okhttp3.internal.http2.StreamResetException
import java.io.File
import javax.inject.Inject


internal const val CHANNEL_UPDATER_NAME = "app updater"
internal const val CHANNEL_UPDATER_ID = "in_app_updater_channel"
internal const val APP_UPDATER_NOTIFICATION_ID = 1
internal const val APP_UPDATE_PROMPT_NOTIFICATION_ID = 2
internal const val EXTRA_UPDATE_URL = "update_url"

@AndroidEntryPoint
class AppUpdaterService : Service() {
    private lateinit var wakeLock: PowerManager.WakeLock

    @Inject
    @IoDispatcher
    lateinit var ioDispatcher: CoroutineDispatcher

    @Inject
    lateinit var client: OkHttpClient

    private lateinit var notifier: AppUpdateNotificationBuilder

    private var downloadingJob: Job? = null
    private var downloadingCall: Call? = null

    override fun onBind(p0: Intent?): IBinder = AppUpdaterBinder()

    /**
     *
     * For instrumented test purposes
     *
     * */
    inner class AppUpdaterBinder : Binder() {
        val service: AppUpdaterService
            get() = this@AppUpdaterService
    }

    override fun onCreate() {
        super.onCreate()
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notifier = AppUpdateNotificationBuilder(this)

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createChannel()
        }

        setupWakeLock()
        startForeground(APP_UPDATER_NOTIFICATION_ID, notifier.onDownloadStarted().build())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) return START_NOT_STICKY

        val url = intent.getStringExtra(EXTRA_UPDATE_URL) ?: return START_NOT_STICKY
        val title = getString(R.string.app_name)

        downloadingJob = CoroutineScope(ioDispatcher).launch {
            downloadApk(title, url)
        }

        downloadingJob?.invokeOnCompletion { stopSelf(startId) }
        return START_NOT_STICKY
    }


    override fun stopService(name: Intent?): Boolean {
        destroyJob()
        return super.stopService(name)
    }

    override fun onDestroy() {
        destroyJob()
    }

    private fun destroyJob() {
        downloadingJob?.cancel()
        downloadingCall?.cancel()
        if (wakeLock.isHeld) {
            wakeLock.release()
        }
    }

    private fun setupWakeLock() {
        val powerManager: PowerManager = getSystemService()!!
        val wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "${javaClass.name}:WakeLock")
        wakeLock.acquire(10*60*1000L /*10 minutes*/)
        this.wakeLock = wakeLock
    }
    
    private suspend fun downloadApk(title: String, url: String) {
        // Show notification download starting.
        notifier.onDownloadStarted(title)

        val progressListener = object : ProgressListener {
            // Progress of the download
            var savedProgress = 0

            // Keep track of the last notification sent to avoid posting too many.
            var lastTick = 0L

            override fun update(bytesRead: Long, contentLength: Long, done: Boolean) {
                val progress = (100 * (bytesRead.toFloat() / contentLength)).toInt()
                val currentTime = System.currentTimeMillis()
                if (progress > savedProgress && currentTime - 200 > lastTick) {
                    savedProgress = progress
                    lastTick = currentTime
                    sendProgressToUpdateActivity(progress)
                    notifier.onProgressChange(progress)
                }
            }
        }

        try {
            // Download the new update.
            val request = Request.Builder()
                .url(url)
                .build()

            val call = client.newCachelessCallWithProgress(request, progressListener)
            downloadingCall = call
            val response = call.await()

            // File where the apk will be saved.
            val apkFile = File(externalCacheDir, "update.apk")

            if (response.isSuccessful) {
                response.body?.source()?.saveTo(apkFile)
            } else {
                response.close()
                throw Exception("Unsuccessful response")
            }

            val uri = apkFile.getUriCompat(this)
            sendInstallUriToUpdateActivity(uri)
            sendProgressToUpdateActivity(100)
            notifier.promptInstall(uri)
        } catch (e: Exception) {
            errorLog(e.stackTraceToString())
            if (e is CancellationException ||
                (e is StreamResetException && e.errorCode == ErrorCode.CANCEL)
            ) {
                notifier.cancel()
            } else {
                notifier.onDownloadError(url)
                sendProgressToUpdateActivity(-1)
            }
        }
    }

    private fun sendProgressToUpdateActivity(progress: Int) {
        val intent = Intent(UPDATE_PROGRESS_RECEIVER_ACTION).apply {
            putExtra(UPDATE_PROGRESS, progress)
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private fun sendInstallUriToUpdateActivity(uri: Uri) {
        val intent = Intent(UPDATE_PROGRESS_RECEIVER_ACTION).apply {
            putExtra(UPDATE_LOCATION, uri.toString())
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }


    companion object {
        private fun isRunning(context: Context): Boolean {
            val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            @Suppress("DEPRECATION")
            return manager.getRunningServices(Integer.MAX_VALUE)
                .any { (AppUpdaterService::class.java).name == it.service.className }
        }

        fun Context.startAppUpdater(url: String) {
            if (isRunning(this)) return

            val intent = Intent(this, AppUpdaterService::class.java).apply {
                putExtra(EXTRA_UPDATE_URL, url)
            }
            ContextCompat.startForegroundService(this, intent)
        }

        fun Context.stopAppUpdater() {
            stopService(Intent(this, AppUpdaterService::class.java))
        }

        internal fun restartDownload(context: Context, url: String): PendingIntent {
            val intent = Intent(context, AppUpdaterService::class.java).apply {
                putExtra(EXTRA_UPDATE_URL, url)
            }
            return PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }
    }
}