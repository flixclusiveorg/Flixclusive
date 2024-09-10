package com.flixclusive.core.util.android

import okio.BufferedSource
import okio.buffer
import okio.sink
import java.io.File
import java.io.OutputStream


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

fun BufferedSource.saveTo(file: File) {
    try {
        file.parentFile?.mkdirs()
        saveTo(file.outputStream())
    } catch (e: Exception) {
        close()
        file.delete()
        throw e
    }
}

fun BufferedSource.saveTo(stream: OutputStream) {
    use { input ->
        stream.sink().buffer().use {
            it.writeAll(input)
            it.flush()
        }
    }
}