package com.flixclusive.data.provider.util

import android.content.Context
import com.flixclusive.core.util.android.saveTo
import com.flixclusive.core.util.exception.safeCall
import com.flixclusive.core.util.log.errorLog
import com.flixclusive.core.util.network.okhttp.request
import com.flixclusive.data.provider.PROVIDERS_FOLDER
import com.flixclusive.model.provider.ProviderData
import okhttp3.OkHttpClient
import java.io.File
import java.nio.charset.StandardCharsets

/**
 *
 * Deletes recursively
 * */
internal fun rmrf(file: File) {
    if (file.isDirectory) {
        for (child in file.listFiles() ?: emptyArray())
            rmrf(child)
    }
    file.delete()
}

fun Context.provideValidProviderPath(
    providerData: ProviderData
) = File("${filesDir}/$PROVIDERS_FOLDER/${buildValidFilename(providerData.repositoryUrl!!)}/${buildValidFilename(providerData.name)}.flx")

/**
 * Mutate the given filename to make it valid for a FAT filesystem,
 * replacing any invalid characters with "_".
 *
 */
internal fun buildValidFilename(name: String): String {
    if (name.isEmpty() || name == "." || name == "..") {
        return "(invalid)"
    }

    val res = StringBuilder(name.length)
    for (c in name) {
        if (isValidFilenameChar(c)) {
            res.append(c)
        } else {
            res.append('_')
        }
    }

    trimFilename(res)
    return res.toString()
}

private fun isValidFilenameChar(c: Char): Boolean {
    val charByte = c.code.toByte()
    // Control characters (0x00 to 0x1F) are not allowed
    if (charByte in 0x00..0x1F) {
        return false
    }
    // Specific characters are also not allowed
    if (c.code == 0x7F) {
        return false
    }

    return when (c) {
        '"', '*', '/', ':', '<', '>', '?', '\\', '|' -> false
        else -> true
    }
}

private fun trimFilename(res: StringBuilder, maxBytes: Int = 254) {
    var rawBytes = res.toString().toByteArray(StandardCharsets.UTF_8)
    if (rawBytes.size > maxBytes) {
        // Reduce max bytes to account for "..."
        val adjustedMaxBytes = maxBytes - 3
        while (rawBytes.size > adjustedMaxBytes) {
            // Remove character from the middle
            res.deleteCharAt(res.length / 2)
            rawBytes = res.toString().toByteArray(StandardCharsets.UTF_8)
        }
        // Insert "..." in the middle
        res.insert(res.length / 2, "...")
    }
}

fun OkHttpClient.downloadFile(
    file: File,
    downloadUrl: String
): Boolean {
    return safeCall {
        file.mkdirs()

        if (file.exists()) {
            file.delete()
        }

        file.createNewFile()

        val response = request(downloadUrl).execute()
        if (!response.isSuccessful) {
            errorLog("Error on download: [${response.code}] ${response.message}")
            return@safeCall false
        }

        response.body!!.source().saveTo(file)
        response.close()

        true
    } ?: false
}

