package com.flixclusive.domain.downloads.usecase

import com.flixclusive.data.downloads.model.DownloadState
import com.flixclusive.domain.downloads.model.DownloadRequest
import kotlinx.coroutines.flow.Flow

interface DownloadFileUseCase {
    /**
     * Initiates a file download based on the provided [request] and returns a [Flow] of [DownloadState]
     * to monitor the download progress and status.
     *
     * @param request The [DownloadRequest] containing details about the file to be downloaded.
     * @return A [Flow] emitting [DownloadState] updates during the download process.
     * */
    operator fun invoke(request: DownloadRequest): Flow<DownloadState>
}
