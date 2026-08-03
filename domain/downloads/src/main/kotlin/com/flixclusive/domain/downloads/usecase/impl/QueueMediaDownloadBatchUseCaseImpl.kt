package com.flixclusive.domain.downloads.usecase.impl

import com.flixclusive.core.common.domain.Async
import com.flixclusive.domain.downloads.controller.MediaDownloadController
import com.flixclusive.domain.downloads.model.MediaDownloadRequest
import com.flixclusive.domain.downloads.usecase.QueueMediaDownloadBatchUseCase
import com.flixclusive.domain.downloads.usecase.QueueMediaDownloadUseCase
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

internal class QueueMediaDownloadBatchUseCaseImpl @Inject constructor(
    private val queueMediaDownloadUseCase: QueueMediaDownloadUseCase,
    private val mediaDownloadController: MediaDownloadController,
) : QueueMediaDownloadBatchUseCase {
    override suspend fun invoke(requests: List<MediaDownloadRequest>): List<Async<String>> =
        coroutineScope {
            requests
                .map { request ->
                    async {
                        queueMediaDownloadUseCase(request.media, request.episode, request.ownerId)
                    }
                }.map { it.await() }
                .onEach { result ->
                    if (result is Async.Success) {
                        mediaDownloadController.start(result.data)
                    }
                }
        }
}
