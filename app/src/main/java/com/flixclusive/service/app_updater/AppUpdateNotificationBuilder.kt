package com.flixclusive.service.app_updater

import android.content.Context
import android.net.Uri
import androidx.core.app.NotificationCompat
import com.flixclusive.R
import com.flixclusive.service.utils.IntentUtils
import com.flixclusive.service.utils.notificationBuilder
import com.flixclusive.service.utils.notify

class AppUpdateNotificationBuilder(
    private val context: Context
) {

    private val notificationBuilder: NotificationCompat.Builder
        get() = context.notificationBuilder(CHANNEL_UPDATER_ID)


    private fun NotificationCompat.Builder.show(id: Int = APP_UPDATER_NOTIFICATION_ID) {
        context.notify(id, build())
    }

    fun onDownloadStarted(title: String? = null): NotificationCompat.Builder {
        with(notificationBuilder) {
            title?.let { setContentTitle(title) }
            setContentText(context.getString(R.string.download_in_progress))
            setSmallIcon(android.R.drawable.stat_sys_download)
            setOngoing(true)

            clearActions()
            addAction(
                R.drawable.round_close_24,
                context.getString(R.string.cancel),
                AppUpdaterReceiver.cancelUpdateDownloadPendingBroadcast(context),
            )
            show()
        }
        return notificationBuilder
    }

    fun onProgressChange(progress: Int) {
        with(notificationBuilder) {
            setProgress(100, progress, false)
            setSmallIcon(android.R.drawable.stat_sys_download_done)
            setOnlyAlertOnce(true)
            show()
        }
    }

    fun promptInstall(uri: Uri) {
        val installIntent = IntentUtils.installApkActivity(uri)
        val installPendingIntent = IntentUtils.installApkPendingActivity(context, uri)

        with(notificationBuilder) {
            setContentText(context.getString(R.string.download_completed))
            setSmallIcon(android.R.drawable.stat_sys_download_done)
            setOnlyAlertOnce(false)
            setProgress(0, 0, false)
            setContentIntent(installPendingIntent)
            setOngoing(true)

            clearActions()
            addAction(
                R.drawable.round_system_update_alt_24,
                context.getString(R.string.install),
                installPendingIntent,
            )
            addAction(
                R.drawable.round_close_24,
                context.getString(R.string.cancel),
                AppUpdaterReceiver.dismissNotification(context)
            )
            show(APP_UPDATE_PROMPT_NOTIFICATION_ID)
            context.startActivity(installIntent)
        }
    }

    fun onDownloadError(url: String) {
        with(notificationBuilder) {
            setContentText(context.getString(R.string.error_downloading_update))
            setSmallIcon(R.drawable.round_error_outline_24)
            setOnlyAlertOnce(false)
            setProgress(0, 0, false)

            clearActions()
            addAction(
                R.drawable.round_refresh_24,
                context.getString(R.string.retry),
                AppUpdaterService.restartDownload(context, url),
            )
            addAction(
                R.drawable.round_close_24,
                context.getString(R.string.cancel),
                AppUpdaterReceiver.dismissNotification(context),
            )
            show()
        }
    }

    fun cancel() {
        AppUpdaterReceiver.dismissNotification(context)
    }
}