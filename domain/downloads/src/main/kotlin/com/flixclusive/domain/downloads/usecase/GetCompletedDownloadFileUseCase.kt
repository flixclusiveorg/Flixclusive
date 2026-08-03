package com.flixclusive.domain.downloads.usecase

import android.net.Uri
import com.flixclusive.core.database.entity.downloads.DownloadItem

data class CompletedSubtitleFile(
    val uri: Uri,
    val language: String,
)

data class CompletedDownloadFile(
    val uri: Uri,
    val mimeType: String,
    val subtitles: List<CompletedSubtitleFile> = emptyList(),
)

interface GetCompletedDownloadFileUseCase {
    suspend operator fun invoke(item: DownloadItem): CompletedDownloadFile?
}
