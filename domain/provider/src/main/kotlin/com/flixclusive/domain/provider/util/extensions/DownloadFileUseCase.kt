package com.flixclusive.domain.provider.util.extensions

import com.flixclusive.core.common.exception.ExceptionWithUiText
import com.flixclusive.core.util.log.infoLog
import com.flixclusive.domain.downloads.model.DownloadRequest
import com.flixclusive.domain.downloads.usecase.DownloadFileUseCase
import com.flixclusive.domain.provider.util.Constants
import com.flixclusive.model.provider.ProviderMetadata
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.takeWhile
import java.io.File

suspend fun DownloadFileUseCase.downloadProvider(
    file: File,
    metadata: ProviderMetadata,
) {
    val providerDownloadRequest = DownloadRequest.from(
        url = metadata.buildUrl,
        destinationPath = file.parent!!,
        fileName = file.name,
    )

    val slashIndex = metadata.buildUrl.lastIndexOf('/')
    val updaterUrl = if (slashIndex != -1) {
        metadata.buildUrl.substring(0, slashIndex + 1) + Constants.UPDATER_FILE
    } else {
        metadata.buildUrl + Constants.UPDATER_FILE
    }

    val updaterJsonDownloadRequest = DownloadRequest.from(
        url = updaterUrl,
        destinationPath = file.parent!!,
        fileName = Constants.UPDATER_FILE,
    )

    combine(
        invoke(providerDownloadRequest),
        invoke(updaterJsonDownloadRequest)
    ) { provider, updaterJson ->
        provider to updaterJson
    }.takeWhile { (provider, updaterJson) ->
        val exception = provider.error ?: updaterJson.error
        val isFinished = provider.status.isFinished && updaterJson.status.isFinished

        when {
            isFinished && exception != null -> throw ExceptionWithUiText(exception)
            isFinished -> infoLog("Successfully downloaded provider: ${metadata.name} [${file.name}]")
        }

        !isFinished
    }.collect()
}
