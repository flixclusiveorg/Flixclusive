package com.flixclusive.data.downloads.transfer

import com.flixclusive.core.database.entity.downloads.DownloadChunk
import com.flixclusive.core.database.entity.downloads.DownloadChunkStatus
import com.hippo.unifile.UniFile

internal interface MediaTransferEngine {
    suspend fun transfer(
        chunks: List<DownloadChunk>,
        url: String,
        headers: Map<String, String>,
        destinationFile: UniFile,
        shouldInterrupt: () -> Boolean,
        onChunkProgress: suspend (chunkId: Long, bytesDownloaded: Long, status: DownloadChunkStatus) -> Unit,
    ): MediaTransferResult
}
