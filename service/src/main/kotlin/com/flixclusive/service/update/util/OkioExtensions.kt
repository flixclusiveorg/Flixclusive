package com.flixclusive.service.update.util

import okio.BufferedSource
import okio.buffer
import okio.sink
import java.io.File
import java.io.OutputStream

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