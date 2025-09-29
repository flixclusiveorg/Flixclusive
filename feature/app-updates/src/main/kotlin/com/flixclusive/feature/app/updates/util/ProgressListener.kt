package com.flixclusive.feature.app.updates.util

interface ProgressListener {
    fun update(
        bytesRead: Long,
        contentLength: Long,
        done: Boolean,
    )
}
