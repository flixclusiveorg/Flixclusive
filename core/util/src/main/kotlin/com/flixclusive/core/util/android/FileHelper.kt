package com.flixclusive.core.util.android

import java.io.File


/**
 *
 * Get total size of a directory recursively.
 * */
fun getDirectorySize(dir: File): Long {
    var size: Long = 0
    dir.listFiles()?.let {
        for (file in it) {
            size += if (file.isFile) {
                file.length()
            } else getDirectorySize(file)
        }
    }

    return size
}