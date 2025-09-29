package com.flixclusive.core.common.intent

import android.content.Intent
import android.net.Uri

/**
 * Creates an [Intent] to launch an activity for installing an APK from the given [uri].
 *
 * The intent is configured with the following:
 * - Action: [Intent.ACTION_VIEW]
 * - Data and Type: The provided [uri] with MIME type "application/vnd.android.package-archive"
 * - Flags: [Intent.FLAG_ACTIVITY_NEW_TASK] and [Intent.FLAG_GRANT_READ_URI_PERMISSION]
 *
 * @param uri The [Uri] pointing to the APK file.
 * @return An [Intent] that can be used to start the installation activity.
 */
fun createApkInstallIntent(uri: Uri): Intent {
    return Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/vnd.android.package-archive")
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
    }
}
