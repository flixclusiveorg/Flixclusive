package com.flixclusive.domain.downloads.usecase

interface CancelDownloadUseCase {
    /**
     * Cancels the download with the specified [downloadId].
     *
     * @param downloadId The unique identifier of the download to be canceled.
     * */
    operator fun invoke(downloadId: String)
}
