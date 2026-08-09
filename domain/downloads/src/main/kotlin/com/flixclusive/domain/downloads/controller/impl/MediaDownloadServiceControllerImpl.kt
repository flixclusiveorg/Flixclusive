package com.flixclusive.domain.downloads.controller.impl

import android.content.Context
import com.flixclusive.core.util.log.errorLog
import com.flixclusive.domain.downloads.controller.MediaDownloadServiceController
import com.flixclusive.domain.downloads.service.MediaDownloadService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

internal class MediaDownloadServiceControllerImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : MediaDownloadServiceController {
    /**
     * A foreground-service start is refused outright on Android 12+ when the app isn't in a state
     * that allows one (ForegroundServiceStartNotAllowedException, an IllegalStateException) — which
     * a background auto-resume sweep can hit. Degrading to an in-process download beats taking the
     * whole app down; the service comes back the next time one is started from the foreground.
     */
    override fun ensureRunning() {
        try {
            MediaDownloadService.ensureStarted(context)
        } catch (e: IllegalStateException) {
            errorLog("Unable to start the download service: ${e.message}")
        }
    }
}
