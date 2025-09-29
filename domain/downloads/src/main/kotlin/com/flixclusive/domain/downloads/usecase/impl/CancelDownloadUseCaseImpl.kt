package com.flixclusive.domain.downloads.usecase.impl

import com.flixclusive.data.downloads.repository.DownloadRepository
import com.flixclusive.domain.downloads.controller.DownloadServiceController
import com.flixclusive.domain.downloads.usecase.CancelDownloadUseCase
import javax.inject.Inject

internal class CancelDownloadUseCaseImpl
    @Inject
    constructor(
        private val downloadRepository: DownloadRepository,
        private val downloadServiceController: DownloadServiceController,
    ) : CancelDownloadUseCase {
        override fun invoke(downloadId: String) {
            downloadServiceController.cancel(downloadId)
            downloadRepository.cancelDownload(downloadId)
        }
    }
