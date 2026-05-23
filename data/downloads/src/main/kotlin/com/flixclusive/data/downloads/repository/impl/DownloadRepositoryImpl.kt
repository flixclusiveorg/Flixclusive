package com.flixclusive.data.downloads.repository.impl

import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.common.locale.UiText
import com.flixclusive.core.network.download.CoroutineDownloader
import com.flixclusive.core.util.log.errorLog
import com.flixclusive.data.downloads.model.DownloadState
import com.flixclusive.data.downloads.model.DownloadStatus
import com.flixclusive.data.downloads.repository.DownloadRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DownloadRepositoryImpl @Inject constructor(
    private val coroutineDownloader: CoroutineDownloader,
    private val appDispatchers: AppDispatchers,
) : DownloadRepository {
    private val states = ConcurrentHashMap<String, MutableStateFlow<DownloadState>>()

    private val jobs = mutableMapOf<String, Job>()

    private val scope by lazy { CoroutineScope(appDispatchers.io + SupervisorJob()) }

    override fun getDownloadState(id: String): Flow<DownloadState> {
        return states
            .computeIfAbsent(id) {
                MutableStateFlow(DownloadState(id = id))
            }.asStateFlow()
            .transformWhile {
                emit(it)
                !it.status.isFinished
            }.onCompletion {
                states.remove(id)
            }
    }

    override fun executeDownload(
        id: String,
        url: String,
        destinationFile: File,
    ) {
        val stateFlow = states.computeIfAbsent(id) {
            MutableStateFlow(DownloadState(id = id))
        }

        jobs[id]?.cancel()
        jobs[id] = scope.launch {
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
            } catch (e: Throwable) {
                errorLog("Download failed for $id: ${e.message}")
                errorLog(e)
                stateFlow.update {
                    it.copy(
                        status = DownloadStatus.FAILED,
                        error = UiText.from(e.message ?: "Unknown error occurred"),
                    )
                }
            } finally {
                states.remove(id)
            }
        }
    }

    override fun cancelDownload(id: String) {
        jobs[id]?.cancel()
        states[id]?.update {
            it.copy(status = DownloadStatus.CANCELLED)
        }
        states.remove(id)
    }
}
