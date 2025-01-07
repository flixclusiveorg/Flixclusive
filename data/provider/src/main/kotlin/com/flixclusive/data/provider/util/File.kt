package com.flixclusive.data.provider.util

import android.content.Context
import com.flixclusive.core.util.android.saveTo
import com.flixclusive.core.util.log.errorLog
import com.flixclusive.core.util.network.okhttp.request
import com.flixclusive.data.provider.PROVIDERS_FOLDER
import com.flixclusive.model.provider.ProviderMetadata
import okhttp3.OkHttpClient
import java.io.File
import java.nio.charset.StandardCharsets
import kotlin.jvm.Throws

/**
 *
 * Deletes recursively
 * */
internal fun rmrf(file: File) {
    if (file.isDirectory) {
        val files = file.listFiles() ?: emptyArray()
        for (subFiles in files) rmrf(subFiles)
    }

    file.delete()
}

internal fun ProviderMetadata.toFile(
    context: Context,
    localPrefix: String? = null,
): File {
    val prefix = localPrefix ?: "${context.filesDir}/$PROVIDERS_FOLDER/"
    val folderName = repositoryUrl.toValidFilename()
    val fileName = id.toValidFilename()

    return File("$prefix$folderName/$fileName.flx")
}
// TODO: Add datastore migration for this file path change

/**
 * Mutate the given filename to make it valid for a FAT filesystem,
 * replacing any invalid characters with "_".
 *
 */
internal fun String.toValidFilename(): String {
    if (isEmpty() || this == "." || this == "..") {
        return "(invalid)"
    }

    val res = StringBuilder(length)
    for (c in this) {
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

private fun trimFilename(
    res: StringBuilder,
    maxBytes: Int = 254,
) {
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

@Throws(DownloadFailed::class)
internal fun OkHttpClient.download(
    file: File,
    downloadUrl: String,
) {
    try {
        val response = request(downloadUrl).execute()
        if (!response.isSuccessful) {
            errorLog("Error on download: [${response.code}] ${response.message}")
            return
        }

        var backupFile: File? = null
        file.mkdirs()
        if (file.exists()) {
            backupFile = File(file.parent!!.plus("${file.name}.old"))
            file.renameTo(backupFile)
            file.delete()
        }

        if (file.createNewFile()) {
            backupFile?.renameTo(file)
            errorLog("Error creating file: $file")
            throw Exception("Error creating file: $file")
        }

        response.body.source().saveTo(file)
        response.close()
        backupFile?.delete()
    } catch (e: Throwable) {
        errorLog("Error on downloading URL: $downloadUrl")
        errorLog(e)

        throw DownloadFailed(downloadUrl)
    }
}

class DownloadFailed(
    downloadUrl: String,
) : Exception("Failed to download the following URL: $downloadUrl")

private const val OAT_FILENAME = "oat"
private const val CLASSES_DEX_FILENAME = "classes.dex"
private const val JSON_SUFFIX = ".json"
private const val PROVIDER_FILE_SUFFIX = ".flx"

val File.isNotOat: Boolean
    get() = name.equals(OAT_FILENAME, true)

val File.isClassesDex: Boolean
    get() = name.equals(CLASSES_DEX_FILENAME, true)

val File.isJson: Boolean
    get() = name.endsWith(JSON_SUFFIX, true)

val File.isProviderFile: Boolean
    get() = name.endsWith(PROVIDER_FILE_SUFFIX, true)
