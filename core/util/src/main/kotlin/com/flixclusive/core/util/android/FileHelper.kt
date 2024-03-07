package com.flixclusive.core.util.android

import java.io.BufferedInputStream
import java.io.File
import java.io.InputStream
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

/**
 *
 * @see [Cloudstream](https://github.com/recloudstream/cloudstream/blob/809a38507bdfc73262d0dd9e6da8c6cc1028341f/app/src/main/java/com/lagradost/cloudstream3/plugins/RepositoryManager.kt#L201)
 * */
fun write(stream: InputStream, output: OutputStream) {
    val input = BufferedInputStream(stream)
    val dataBuffer = ByteArray(512)
    var readBytes: Int
    while (input.read(dataBuffer).also { readBytes = it } != -1) {
        output.write(dataBuffer, 0, readBytes)
    }
}