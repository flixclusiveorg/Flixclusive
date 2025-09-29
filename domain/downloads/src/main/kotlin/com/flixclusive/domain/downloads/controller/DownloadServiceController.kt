package com.flixclusive.domain.downloads.controller

import com.flixclusive.domain.downloads.model.DownloadRequest

/**
 * A wrapper interface to control the [DownloadService].
 * */
interface DownloadServiceController {
    /**
     * Starts the download service with the given [request].
     * */
    fun start(request: DownloadRequest)

    /**
     * Stops the download service.
     * */
    fun cancel(downloadId: String)
}
