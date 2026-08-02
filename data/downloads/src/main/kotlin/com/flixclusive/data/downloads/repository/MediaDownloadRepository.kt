package com.flixclusive.data.downloads.repository

import com.flixclusive.core.database.entity.downloads.DownloadItem
import com.flixclusive.core.database.entity.downloads.DownloadItemState
import com.flixclusive.core.database.entity.downloads.DownloadPhase
import com.flixclusive.data.downloads.model.DownloadInterruptReason
import com.flixclusive.data.downloads.transfer.MediaTransferResult
import com.hippo.unifile.UniFile
import kotlinx.coroutines.flow.Flow

interface MediaDownloadRepository {
    fun observeItem(id: Long): Flow<DownloadItem?>

    fun observeAllItems(): Flow<List<DownloadItem>>

    suspend fun getItem(id: Long): DownloadItem?

    suspend fun queue(item: DownloadItem): Long

    suspend fun getOldestQueuedItem(): DownloadItem?

    suspend fun getBatch(
        mediaId: String,
        seasonNumber: Int,
    ): List<DownloadItem>

    fun observeBatch(
        mediaId: String,
        seasonNumber: Int,
    ): Flow<List<DownloadItem>>

    suspend fun updateState(
        id: Long,
        state: DownloadItemState,
        phase: DownloadPhase?,
    )

    suspend fun markError(
        id: Long,
        message: String,
    )

    suspend fun markSubtitleError(
        id: Long,
        message: String,
    )

    suspend fun resetChunks(id: Long)

    suspend fun runTransfer(
        id: Long,
        phase: DownloadPhase,
        url: String,
        headers: Map<String, String>,
        destinationFile: UniFile,
        totalBytes: Long?,
    ): MediaTransferResult

    fun requestInterrupt(
        id: Long,
        reason: DownloadInterruptReason,
    )

    fun consumeInterruptReason(id: Long): DownloadInterruptReason?

    suspend fun delete(id: Long)
}
