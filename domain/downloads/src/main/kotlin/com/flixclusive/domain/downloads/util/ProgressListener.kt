package com.flixclusive.domain.downloads.util

internal interface ProgressListener {
    fun update(
        bytesRead: Long,
        contentLength: Long,
        done: Boolean,
    )
}
