package com.flixclusive.core.common.file

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
