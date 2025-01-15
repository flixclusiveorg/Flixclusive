package com.flixclusive.core.datastore.util

import android.content.Context
import com.flixclusive.core.datastore.PROVIDERS_FOLDER_NAME
import com.flixclusive.core.datastore.PROVIDERS_SETTINGS_FOLDER_NAME
import java.io.File

/**
 *
 * Deletes recursively
 * */
fun rmrf(file: File) {
    if (file.isDirectory) {
        val files = file.listFiles() ?: emptyArray()
        for (subFiles in files) rmrf(subFiles)
    }

    file.delete()
}

fun Context.getExternalDirPath(): String? {
    val externalDir = getExternalFilesDir(null)
    val externalDirPath = externalDir?.absolutePath

    return externalDirPath
}

fun Context.getProvidersPathPrefix(userId: Int): String = getExternalDirPath() + "/$PROVIDERS_FOLDER_NAME/user-$userId"

fun Context.getProvidersSettingsPathPrefix(userId: Int): String =
    getExternalDirPath() + "/$PROVIDERS_SETTINGS_FOLDER_NAME/user-$userId"
