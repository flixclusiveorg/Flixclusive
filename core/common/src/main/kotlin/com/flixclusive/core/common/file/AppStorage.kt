package com.flixclusive.core.common.file

import android.os.Environment
import java.io.File

object AppStorage {
    private const val DEFAULT_FOLDER_NAME = "Flixclusive"
    private const val BACKUP_FOLDER_NAME = "backups"

    /**
     * Returns the public Downloads directory.
     * */
    fun getPublicDownloadsDirectory(): File {
        return File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            DEFAULT_FOLDER_NAME,
        )
    }

    fun getPublicDirectory(): File {
        return File(
            Environment.getExternalStorageDirectory(),
            DEFAULT_FOLDER_NAME,
        )
    }

    fun getPublicBackupDirectory(): File {
        return File(
            getPublicDirectory(),
            BACKUP_FOLDER_NAME,
        )
    }
}
