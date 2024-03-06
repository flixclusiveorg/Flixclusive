package com.flixclusive.data.provider.util

import java.io.File

/**
 *
 * Deletes OAT recursively
 * */
fun rmrf(file: File) {
    if (file.isDirectory) {
        for (child in file.listFiles() ?: emptyArray())
            rmrf(child)
    }
    file.delete()
}