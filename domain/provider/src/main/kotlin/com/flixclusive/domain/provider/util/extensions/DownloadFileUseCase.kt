package com.flixclusive.domain.provider.util.extensions

import com.flixclusive.core.common.provider.ProviderConstants
import com.flixclusive.data.downloads.model.DownloadState
import com.flixclusive.data.downloads.model.DownloadStatus
import com.flixclusive.domain.downloads.model.DownloadRequest
import com.flixclusive.domain.downloads.usecase.DownloadFileUseCase
import com.flixclusive.model.provider.ProviderMetadata
import java.io.File

suspend fun DownloadFileUseCase.downloadProvider(
    file: File,
    metadata: ProviderMetadata,
    onStateChange: (DownloadState) -> Unit,
) {
    val providerDownloadRequest = DownloadRequest.from(
        url = metadata.buildUrl,
        destinationPath = file.parent!!,
        fileName = file.name,
    )

    val slashIndex = metadata.buildUrl.lastIndexOf('/')
    val updaterUrl = if (slashIndex != -1) {
        metadata.buildUrl.substring(0, slashIndex + 1) + ProviderConstants.UPDATER_JSON_FILE
    } else {
        metadata.buildUrl + ProviderConstants.UPDATER_JSON_FILE
    }

    val updaterJsonDownloadRequest = DownloadRequest.from(
        url = updaterUrl,
        destinationPath = file.parent!!,
        fileName = ProviderConstants.UPDATER_JSON_FILE,
    )

    invoke(updaterJsonDownloadRequest).collect {
        onStateChange(
            it.copy(
                status = if (it.status == DownloadStatus.COMPLETED) DownloadStatus.DOWNLOADING else it.status,
                progress = it.progress * 0.2f
            )
        )
    }

    invoke(providerDownloadRequest).collect {
        onStateChange(
            it.copy(
                progress = (it.progress * 0.8f) + 20f
            )
        )
    }
}
