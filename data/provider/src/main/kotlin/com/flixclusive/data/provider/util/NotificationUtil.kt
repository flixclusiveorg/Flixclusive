package com.flixclusive.data.provider.util

import android.content.Context
import androidx.core.app.NotificationCompat
import com.flixclusive.core.util.android.notify
import com.flixclusive.model.provider.ProviderData
import com.flixclusive.core.locale.R as LocaleR

object NotificationUtil {
    // For error notifications
    private const val CHANNEL_LOAD_ERROR_ID = "PROVIDER_LOAD_ERROR_CHANNEL_ID"
    private const val CHANNEL_LOAD_ERROR_NAME = "PROVIDER_ERROR_LOAD_CHANNEL"
    // ====

    fun Context.notifyOnError(
        shouldInitializeChannel: Boolean,
        providers: Collection<ProviderData>
    ) {
        val failedToLoadProviders = providers.joinToString(", ") { it.name }
        val notificationBody = getString(LocaleR.string.failed_to_load_providers_msg_format, failedToLoadProviders)

        notify(
            id = (System.currentTimeMillis() / 1000).toInt(),
            channelId = CHANNEL_LOAD_ERROR_ID,
            channelName = CHANNEL_LOAD_ERROR_NAME,
            shouldInitializeChannel = shouldInitializeChannel,
        ) {
            setContentTitle(getString(LocaleR.string.failed_to_load_providers))
            setContentText(notificationBody)
            setSmallIcon(com.flixclusive.core.ui.common.R.drawable.round_error_outline_24)
            setOnlyAlertOnce(false)
            setAutoCancel(true)
            setColorized(true)
            setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(notificationBody)
            )
        }
    }
}