package com.flixclusive.domain.provider.util.extensions

import com.flixclusive.core.common.provider.ProviderConstants
import com.flixclusive.core.util.android.saveTo
import com.flixclusive.core.util.log.errorLog
import com.flixclusive.core.util.network.okhttp.request
import okhttp3.OkHttpClient
import java.io.File

@Throws(DownloadFailed::class)
internal fun OkHttpClient.downloadProvider(
    saveTo: File,
    buildUrl: String,
) {
    val updaterJsonUrl = getUpdaterJsonUrl(buildUrl)
    val updaterFile = File(saveTo.parent!!.plus("/${ProviderConstants.UPDATER_JSON_FILE}"))

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
        url.substring(0, slashIndex + 1) + ProviderConstants.UPDATER_JSON_FILE
    } else {
        url + ProviderConstants.UPDATER_JSON_FILE
    }
}
