package com.flixclusive.service.app_updater

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.flixclusive.service.app_updater.AppUpdaterService.Companion.startAppUpdater
import com.flixclusive.service.app_updater.AppUpdaterService.Companion.stopAppUpdater
import com.flixclusive.service.utils.cancelNotification

class AppUpdaterReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        when(intent?.getStringExtra(RECEIVER_MSG)) {
            ACTION_DISMISS_NOTIFICATION -> {
                context?.cancelNotification(
                    APP_UPDATE_PROMPT_NOTIFICATION_ID
                )
            }
            ACTION_CANCEL_APP_UPDATE_DOWNLOAD -> context?.stopAppUpdater()
            ACTION_START_APP_UPDATE_DOWNLOAD -> {
                intent.getStringExtra(EXTRA_UPDATE_URL)?.let {
                    context?.startAppUpdater(it)
                }
            }
        }
    }

    companion object {
        // Actions
        private const val ACTION_DISMISS_NOTIFICATION = "dismiss"
        private const val ACTION_CANCEL_APP_UPDATE_DOWNLOAD = "cancel_update"
        private const val ACTION_START_APP_UPDATE_DOWNLOAD = "start_update"

        private const val RECEIVER_MSG = "receiver_msg"

        internal fun dismissNotification(context: Context): PendingIntent {
            val intent = Intent(context, AppUpdaterReceiver::class.java)
            intent.putExtra(RECEIVER_MSG, ACTION_DISMISS_NOTIFICATION)
            return PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        internal fun cancelUpdateDownloadPendingBroadcast(context: Context): PendingIntent {
            val intent = Intent(context, AppUpdaterReceiver::class.java)
            intent.putExtra(RECEIVER_MSG, ACTION_CANCEL_APP_UPDATE_DOWNLOAD)
            return PendingIntent.getBroadcast(
                context,
                1,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}