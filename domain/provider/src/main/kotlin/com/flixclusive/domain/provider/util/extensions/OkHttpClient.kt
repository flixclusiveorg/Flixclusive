package com.flixclusive.domain.provider.util.extensions

import com.flixclusive.core.util.android.saveTo
import com.flixclusive.core.util.log.errorLog
import com.flixclusive.core.util.network.okhttp.request
import com.flixclusive.domain.provider.util.Constants
import okhttp3.OkHttpClient
import java.io.File

@Throws(DownloadFailed::class)
internal fun OkHttpClient.downloadProvider(
    saveTo: File,
    buildUrl: String,
) {
    val updaterJsonUrl = getUpdaterJsonUrl(buildUrl)
    val updaterFile = File(saveTo.parent!!.plus("/${Constants.UPDATER_FILE}"))

    download(
        file = saveTo,
        downloadUrl = buildUrl,
    )

    download(
        file = updaterFile,
        downloadUrl = updaterJsonUrl,
    )
}

internal fun OkHttpClient.isUrlOnline(branchUrl: String): Boolean {
    val response = request(branchUrl).execute()

    return response.isSuccessful
}

@Throws(DownloadFailed::class)
private fun OkHttpClient.download(
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
            backupFile = File(file.parent!!.plus("/${file.name}.old"))
            file.renameTo(backupFile)
            file.delete()
        }

        if (!file.createNewFile()) {
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

private fun getUpdaterJsonUrl(url: String): String {
    val slashIndex = url.lastIndexOf('/')
    return if (slashIndex != -1) {
        url.substring(0, slashIndex + 1) + Constants.UPDATER_FILE
    } else {
        url + Constants.UPDATER_FILE
    }
}
