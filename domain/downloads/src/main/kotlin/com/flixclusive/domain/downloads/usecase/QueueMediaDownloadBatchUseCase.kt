package com.flixclusive.domain.downloads.usecase

import com.flixclusive.domain.downloads.model.MediaDownloadRequest

interface QueueMediaDownloadBatchUseCase {
    suspend operator fun invoke(requests: List<MediaDownloadRequest>): List<String>
}
