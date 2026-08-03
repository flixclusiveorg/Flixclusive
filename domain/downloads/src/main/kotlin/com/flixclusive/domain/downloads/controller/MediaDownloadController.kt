package com.flixclusive.domain.downloads.controller

interface MediaDownloadController {
    fun start(itemId: String)

    fun pause(itemId: String)

    fun resume(itemId: String)

    fun stop(itemId: String)

    fun retry(itemId: String)

    fun delete(itemId: String)

    fun pauseBatch(
        mediaId: String,
        seasonNumber: Int,
    )

    fun stopBatch(
        mediaId: String,
        seasonNumber: Int,
    )
}
