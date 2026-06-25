package com.flixclusive.domain.provider.util.extensions

import com.flixclusive.core.util.network.json.fromJson
import dalvik.system.PathClassLoader
import java.io.InputStreamReader

internal inline fun <reified T> PathClassLoader.getFileFromPath(file: String): T {
    val manifest: T

    getResourceAsStream(file).use { stream ->
        if (stream == null) {
            throw NullPointerException("No $file found")
        }

        InputStreamReader(stream).use { reader ->
            manifest = fromJson(reader)
        }
    }

    return manifest
}
