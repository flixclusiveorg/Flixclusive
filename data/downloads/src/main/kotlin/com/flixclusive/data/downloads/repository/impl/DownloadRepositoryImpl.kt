package com.flixclusive.data.downloads.repository.impl

import com.flixclusive.core.common.locale.UiText
import com.flixclusive.core.network.download.CoroutineDownloader
import com.flixclusive.data.downloads.model.DownloadState
import com.flixclusive.data.downloads.model.DownloadStatus
import com.flixclusive.data.downloads.repository.DownloadRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DownloadRepositoryImpl
    @Inject
    constructor(
        private val coroutineDownloader: CoroutineDownloader,
    ) : DownloadRepository {
        private val downloadStates = mutableMapOf<String, MutableStateFlow<DownloadState>>()

        override fun getDownloadState(downloadId: String): StateFlow<DownloadState> {
            return downloadStates
                .getOrPut(downloadId) {
                    MutableStateFlow(DownloadState(downloadId = downloadId))
                }.asStateFlow()
        }

        override suspend fun executeDownload(
            downloadId: String,
            url: String,
            destinationFile: File,
        ) {
            val stateFlow = downloadStates.getOrPut(downloadId) {
                MutableStateFlow(DownloadState(downloadId = downloadId))
            }

            try {
                stateFlow.update { it.copy(status = DownloadStatus.DOWNLOADING) }

                coroutineDownloader
                    .download(url, destinationFile)
                    .collectLatest { progress ->
                        stateFlow.update {
                            it.copy(
                                progress = progress.progress,
                                bytesDownloaded = progress.bytesDownloaded,
                                totalBytes = progress.totalBytes,
                                file = destinationFile,
                                status = when {
                                    progress.isComplete -> DownloadStatus.COMPLETED
                                    else -> DownloadStatus.DOWNLOADING
                                },
                            )
                        }
                    }
            } catch (e: Exception) {
                stateFlow.update {
                    it.copy(
                        status = DownloadStatus.FAILED,
                        error = UiText.from(e.message ?: "Unknown error occurred"),
                    )
                }
            }
        }

        override fun cancelDownload(downloadId: String) {
            downloadStates[downloadId]?.update {
                it.copy(status = DownloadStatus.CANCELLED)
            }
        }
    }
