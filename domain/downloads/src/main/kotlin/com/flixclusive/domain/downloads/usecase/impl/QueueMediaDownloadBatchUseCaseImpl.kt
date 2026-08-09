package com.flixclusive.domain.downloads.usecase.impl

import com.flixclusive.domain.downloads.model.MediaDownloadRequest
import com.flixclusive.domain.downloads.usecase.QueueMediaDownloadBatchUseCase
import com.flixclusive.domain.downloads.usecase.QueueMediaDownloadUseCase
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

internal class QueueMediaDownloadBatchUseCaseImpl @Inject constructor(
    private val queueMediaDownloadUseCase: QueueMediaDownloadUseCase,
) : QueueMediaDownloadBatchUseCase {
    override suspend fun invoke(requests: List<MediaDownloadRequest>): List<String> =
        coroutineScope {
            requests
                .map { request ->
                    async { queueMediaDownloadUseCase(request.media, request.episode, request.ownerId) }
                }.awaitAll()
        }
}
