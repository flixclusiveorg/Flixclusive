package com.flixclusive.core.ui.player.util

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.HttpDataSource
import com.flixclusive.core.locale.UiText
import com.flixclusive.core.util.log.errorLog

internal fun String?.isDdosProtection(): Boolean {
    if (this == null) return true

    return contains("ddos", true)
    || contains("cloudflare", true)
}

@OptIn(UnstableApi::class)
internal fun PlaybackException.handleError(
    duration: Long?,
    showErrorCallback: (UiText) -> Unit,
    rePreparePlayerCallback: () -> Unit
) {
    val isNetworkException =
        errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED
                || errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT
    when {
        isNetworkException
            && duration != null
            && duration != C.TIME_UNSET -> {
            rePreparePlayerCallback()
        }

        errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW -> {
            rePreparePlayerCallback()
        }

        else -> {
            var message = localizedMessage

            if (cause is HttpDataSource.InvalidResponseCodeException) {
                val okHttpError = cause as HttpDataSource.InvalidResponseCodeException
                val responseBody = String(okHttpError.responseBody)

                errorLog(
                    """
                            Headers: ${okHttpError.dataSpec.httpRequestHeaders}
                            Url: ${okHttpError.dataSpec.uri}
                            Body: $responseBody
                        """.trimIndent()
                )

                if (responseBody.isDdosProtection())
                    message = "Anti-DDoS"
            }

            val errorMessage = UiText.StringValue("PlaybackException [${errorCode}]: $message")

            showErrorCallback(errorMessage)
        }
    }
}