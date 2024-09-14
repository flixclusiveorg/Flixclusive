package com.flixclusive.service.update

import android.app.ActivityManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import com.flixclusive.core.ui.common.util.showToast
import com.flixclusive.core.util.android.createChannel
import com.flixclusive.core.util.android.notificationManager
import com.flixclusive.core.util.android.saveTo
import com.flixclusive.core.util.coroutines.AppDispatchers
import com.flixclusive.core.util.log.errorLog
import com.flixclusive.data.configuration.AppConfigurationManager
import com.flixclusive.service.update.util.ProgressListener
import com.flixclusive.service.update.util.await
import com.flixclusive.service.update.util.getUriCompat
import com.flixclusive.service.update.util.newCachelessCallWithProgress
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.internal.http2.ErrorCode
import okhttp3.internal.http2.StreamResetException
import java.io.File
import javax.inject.Inject
import com.flixclusive.core.locale.R as LocaleR

internal const val CHANNEL_UPDATER_NAME = "app updater"
internal const val CHANNEL_UPDATER_ID = "in_app_updater_channel"
internal const val APP_UPDATER_NOTIFICATION_ID = 1
internal const val APP_UPDATE_PROMPT_NOTIFICATION_ID = 2
internal const val EXTRA_UPDATE_URL = "update_url"

@AndroidEntryPoint
class AppUpdaterService : Service() {
    private lateinit var wakeLock: PowerManager.WakeLock

    @Inject
    lateinit var client: OkHttpClient

    @Inject
    lateinit var appConfigurationManager: AppConfigurationManager

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
        notifier = AppUpdateNotificationBuilder(this)

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createChannel(
                channelId = CHANNEL_UPDATER_ID,
                channelName = CHANNEL_UPDATER_NAME
            )
        }

        setupWakeLock()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(APP_UPDATER_NOTIFICATION_ID, notifier.onDownloadStarted().build(), FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(APP_UPDATER_NOTIFICATION_ID, notifier.onDownloadStarted().build())
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) return START_NOT_STICKY

        val url = intent.getStringExtra(EXTRA_UPDATE_URL) ?: return START_NOT_STICKY
        val title = getString(LocaleR.string.app_name)

        downloadingJob = AppDispatchers.Default.scope.launch {
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
                    sendDownloadProgress(progress)
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
                response.close()
            } else {
                response.close()
                throw Exception("Unsuccessful response")
            }

            val uri = apkFile.getUriCompat(
                applicationId = appConfigurationManager.currentAppBuild!!.applicationId,
                context = this
            )
            sendInstallUriLocation(uri)
            sendDownloadProgress(100)
            notifier.promptInstall(uri)
        } catch (e: Exception) {
            errorLog(e)
            if (e is CancellationException ||
                (e is StreamResetException && e.errorCode == ErrorCode.CANCEL)
            ) {
                notifier.cancel()
            } else {
                notifier.onDownloadError(url)
                // Run on ui thread
                Handler(Looper.getMainLooper()).post {
                    showToast(
                        message = getString(LocaleR.string.failed_to_download_updates, e.localizedMessage),
                        duration = Toast.LENGTH_LONG
                    )
                }
                sendDownloadProgress(null)
            }
        }
    }

    private fun sendDownloadProgress(progress: Int?) {
        _downloadProgress.value = progress
    }

    private fun sendInstallUriLocation(uri: Uri) {
        _installUriLocation.value = uri
    }


    companion object {
        private val _downloadProgress = MutableStateFlow<Int?>(null)
        val downloadProgress = _downloadProgress.asStateFlow()

        private val _installUriLocation = MutableStateFlow<Uri?>(null)
        val installUriLocation = _installUriLocation.asStateFlow()

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