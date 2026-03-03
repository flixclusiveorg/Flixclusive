package com.flixclusive.core.presentation.player.extensions

import androidx.annotation.OptIn
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.HttpDataSource
import com.flixclusive.core.common.locale.UiText
import com.flixclusive.core.presentation.player.R
import com.flixclusive.core.util.log.errorLog

internal fun String?.isDdosProtection(): Boolean {
    if (this == null) return true

    return contains("ddos", true) ||
        contains("cloudflare", true)
}

fun PlaybackException.isNetworkException() =
    errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ||
        errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT

fun PlaybackException.isLiveError() = errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW

@OptIn(UnstableApi::class)
fun PlaybackException.getFormatMessage(): UiText {
    var message: UiText = UiText.from(localizedMessage ?: "Unknown error")

    if (isLiveError()) {
        message = UiText.from(R.string.live_stream_error_message)
    } else if (isNetworkException()) {
        message = UiText.from(R.string.network_error_message)
    } else if (cause is HttpDataSource.InvalidResponseCodeException) {
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
            message = UiText.from(R.string.anti_ddos_message)
        }
    }

    return UiText.from("ERR [$errorCode]: $message")
}
