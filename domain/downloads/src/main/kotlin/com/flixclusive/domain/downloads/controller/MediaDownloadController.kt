package com.flixclusive.domain.downloads.controller

interface MediaDownloadController {
    fun start(itemId: Long)

    fun pause(itemId: Long)

    fun resume(itemId: Long)

    fun stop(itemId: Long)

    fun retry(itemId: Long)

    fun delete(itemId: Long)
}
