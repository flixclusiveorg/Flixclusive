package com.flixclusive.domain.provider.usecase.manage

sealed class DownloadProviderResult {
    data class Downloading(
        val progress: Float,
        val downloadId: String
    ) : DownloadProviderResult()

    data class Failure(
        val error: Throwable
    ) : DownloadProviderResult()

    data object Success : DownloadProviderResult()
}
