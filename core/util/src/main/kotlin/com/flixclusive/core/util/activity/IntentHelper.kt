package com.flixclusive.core.util.activity

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri

fun installApkPendingActivity(context: Context, uri: Uri): PendingIntent {
    val intent = installApkActivity(uri)

    return PendingIntent.getActivity(
        /* context = */ context,
        /* requestCode = */ 0,
        /* intent = */ intent,
        /* flags = */ PendingIntent.FLAG_IMMUTABLE
    )
}

fun installApkActivity(uri: Uri): Intent {
    val mime = "application/vnd.android.package-archive"
    return Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, mime)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
    }
}