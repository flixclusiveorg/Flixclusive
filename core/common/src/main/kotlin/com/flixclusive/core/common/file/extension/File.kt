package com.flixclusive.core.common.file.extension

import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import java.io.File
import java.io.IOException

fun File.toUri(
    applicationId: String,
    context: Context,
): Uri {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        FileProvider.getUriForFile(context, "$applicationId.storage_provider", this)
    } else {
        toUri()
    }
}

fun File.isEmpty(): Boolean {
    return if (!isDirectory) !exists() else listFiles()?.isEmpty() ?: true
}

fun File.isWritableDirectory(): Boolean {
    try {
        if (!isDirectory && !mkdirs()) return false

        val probe = File(this, ".write-probe-${System.nanoTime()}")
        if (!probe.createNewFile()) return false

        probe.delete()
        return true
    } catch (_: IOException) {
        return false
    } catch (_: SecurityException) {
        return false
    }
}
