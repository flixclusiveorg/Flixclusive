package com.flixclusive.core.presentation.player.extensions

import androidx.annotation.OptIn
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.HttpDataSource
import com.flixclusive.core.util.log.errorLog

internal fun String?.isDdosProtection(): Boolean {
    if (this == null) return true

    return contains("ddos", true) ||
        contains("cloudflare", true)
}

internal fun PlaybackException.isNetworkException() =
    errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ||
        errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT

internal fun PlaybackException.isLiveError() = errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW

@OptIn(UnstableApi::class)
internal fun PlaybackException.getFormatMessage(): String {
    var message = localizedMessage

    if (cause is HttpDataSource.InvalidResponseCodeException) {
        val okHttpError = cause as HttpDataSource.InvalidResponseCodeException
        val responseBody = String(okHttpError.responseBody)

        errorLog(
            """
            Headers: ${okHttpError.dataSpec.httpRequestHeaders}
            Url: ${okHttpError.dataSpec.uri}
            Body: $responseBody
            """.trimIndent(),
        )

        if (responseBody.isDdosProtection()) {
            message = "Anti-DDoS"
        }
    }

    return "PlaybackException [$errorCode]: $message"
}
