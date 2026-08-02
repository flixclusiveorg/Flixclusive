package com.flixclusive.domain.downloads.controller.impl

import android.content.Context
import com.flixclusive.domain.downloads.controller.MediaDownloadServiceController
import com.flixclusive.domain.downloads.service.MediaDownloadService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

internal class MediaDownloadServiceControllerImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : MediaDownloadServiceController {
    override fun ensureRunning() {
        MediaDownloadService.ensureStarted(context)
    }
}
