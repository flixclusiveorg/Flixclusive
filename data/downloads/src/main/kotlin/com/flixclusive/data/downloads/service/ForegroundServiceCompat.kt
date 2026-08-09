package com.flixclusive.data.downloads.service

import android.app.Notification
import android.app.Service
import android.content.pm.ServiceInfo
import android.os.Build

/**
 * Starts [Service] in the foreground, declaring the `dataSync` type where the platform requires it.
 *
 * API 29 made the type argument mandatory for a service that declares one in the manifest; calling
 * the two-argument overload there throws.
 */
fun Service.safeStartForeground(
    id: Int,
    notification: Notification,
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        startForeground(id, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
    } else {
        startForeground(id, notification)
    }
}

/** Leaves the foreground and removes the notification, across the API 24 signature change. */
fun Service.stopForegroundCompat() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        stopForeground(Service.STOP_FOREGROUND_REMOVE)
    } else {
        @Suppress("DEPRECATION")
        stopForeground(true)
    }
}
