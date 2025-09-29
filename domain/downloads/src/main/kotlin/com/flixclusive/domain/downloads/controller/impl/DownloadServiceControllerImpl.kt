package com.flixclusive.domain.downloads.controller.impl

import android.content.Context
import com.flixclusive.data.downloads.service.DownloadService
import com.flixclusive.domain.downloads.controller.DownloadServiceController
import com.flixclusive.domain.downloads.model.DownloadRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

internal class DownloadServiceControllerImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : DownloadServiceController {
        override fun start(request: DownloadRequest) {
            DownloadService.startDownload(
                context = context,
                downloadId = request.downloadId,
                url = request.url,
                filePath = request.destinationPath,
                fileName = request.fileName,
            )
        }

        override fun cancel(downloadId: String) {
            DownloadService.cancelDownload(context, downloadId)
        }
    }
