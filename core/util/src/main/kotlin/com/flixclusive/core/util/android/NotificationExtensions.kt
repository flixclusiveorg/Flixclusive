package com.flixclusive.core.util.android

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.NotificationManagerCompat.NotificationWithIdAndTag
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.core.content.getSystemService
import com.flixclusive.core.util.R

val Context.notificationManager: NotificationManager
    get() = getSystemService()!!

fun Context.notify(
    id: Int,
    channelId: String,
    block: (NotificationCompat.Builder.() -> Unit)? = null,
) {
    val notification = notificationBuilder(channelId, block).build()
    this.notify(id, notification)
}

fun Context.notify(id: Int, notification: Notification) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && PermissionChecker.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) != PermissionChecker.PERMISSION_GRANTED
    ) {
        return
    }

    NotificationManagerCompat.from(this).notify(id, notification)
}

fun Context.notify(notificationWithIdAndTags: List<NotificationWithIdAndTag>) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && PermissionChecker.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) != PermissionChecker.PERMISSION_GRANTED
    ) {
        return
    }

    NotificationManagerCompat.from(this).notify(notificationWithIdAndTags)
}

fun Context.cancelNotification(id: Int) {
    NotificationManagerCompat.from(this).cancel(id)
}

fun Context.notificationBuilder(
    channelId: String, block: (NotificationCompat.Builder.() -> Unit)? = null
): NotificationCompat.Builder {
    val builder = NotificationCompat.Builder(this, channelId)
        .setColor(ContextCompat.getColor(this, R.color.md_theme_dark_primary))
    if (block != null) {
        builder.block()
    }
    return builder
}

@RequiresApi(Build.VERSION_CODES.O)
fun NotificationManager.createChannel(
    channelId: String,
    channelName: String
) {
    val channel = NotificationChannel(
        channelId,
        channelName,
        NotificationManager.IMPORTANCE_DEFAULT
    )

    channel.enableLights(true)
    channel.lightColor = Color.valueOf(0xFF191C1E.toInt()).toArgb()

    createNotificationChannel(channel)
}