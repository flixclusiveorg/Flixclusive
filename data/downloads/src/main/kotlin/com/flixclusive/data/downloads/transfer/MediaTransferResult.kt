package com.flixclusive.data.downloads.transfer

sealed class MediaTransferResult {
    data object Completed : MediaTransferResult()

    data class Failed(
        val cause: Throwable
    ) : MediaTransferResult()

    data object Cancelled : MediaTransferResult()
}
