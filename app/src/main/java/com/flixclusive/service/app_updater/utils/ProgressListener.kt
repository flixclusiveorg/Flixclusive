package com.flixclusive.service.app_updater.utils

interface ProgressListener {
    fun update(bytesRead: Long, contentLength: Long, done: Boolean)
}