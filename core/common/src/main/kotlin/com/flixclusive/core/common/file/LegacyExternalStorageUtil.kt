package com.flixclusive.core.common.file

import android.os.Environment
import java.io.File

object LegacyExternalStorageUtil {
    /**
     * Returns the public Downloads directory.
     * */
    fun getPublicDownloadsDirectory(): File? {
        return File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "Flixclusive",
        )
    }
}
