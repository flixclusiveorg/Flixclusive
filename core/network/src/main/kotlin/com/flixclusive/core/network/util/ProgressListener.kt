package com.flixclusive.core.network.util

interface ProgressListener {
    fun update(
        bytesRead: Long,
        contentLength: Long,
        done: Boolean,
    )
}
