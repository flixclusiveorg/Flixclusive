package com.flixclusive.core.datastore.util

import android.content.Context
import com.flixclusive.core.datastore.PROVIDERS_FOLDER_NAME
import com.flixclusive.core.datastore.PROVIDERS_SETTINGS_FOLDER_NAME
import java.io.File


/**
 * Deletes a file or directory recursively.
 *
 * @param file The file or directory to delete.
 */
fun rmrf(file: File) {
    if (file.isDirectory) {
        val files = file.listFiles() ?: emptyArray()
        for (subFiles in files) rmrf(subFiles)
    }

    file.delete()
}

/**
 * Returns the path prefix for the providers folder for a specific user.
 *
 * @param userId The ID of the user.
 * @return The path prefix for the providers folder.
 */
fun Context.getProvidersPathPrefix(userId: Int): String =
    getExternalFilesDir(null)?.absolutePath + "/$PROVIDERS_FOLDER_NAME/user-$userId"

/**
 * Returns the path prefix for the providers settings folder for a specific user.
 *
 * @param userId The ID of the user.
 * @return The path prefix for the providers settings folder.
 */
internal fun Context.getProvidersSettingsPathPrefix(userId: Int): String =
    getExternalFilesDir(null)?.absolutePath + "/$PROVIDERS_SETTINGS_FOLDER_NAME/user-$userId"
