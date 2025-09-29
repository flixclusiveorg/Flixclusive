package com.flixclusive.domain.downloads.usecase.impl

import com.flixclusive.data.downloads.model.DownloadState
import com.flixclusive.data.downloads.repository.DownloadRepository
import com.flixclusive.domain.downloads.controller.DownloadServiceController
import com.flixclusive.domain.downloads.model.DownloadRequest
import com.flixclusive.domain.downloads.usecase.DownloadFileUseCase
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

internal class DownloadFileUseCaseImpl
    @Inject
    constructor(
        private val downloadRepository: DownloadRepository,
        private val downloadServiceController: DownloadServiceController,
    ) : DownloadFileUseCase {
        override fun invoke(request: DownloadRequest): Flow<DownloadState> {
            downloadServiceController.start(request)
            return downloadRepository.getDownloadState(request.downloadId)
        }
    }
