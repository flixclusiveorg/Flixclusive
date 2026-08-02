package com.flixclusive.domain.downloads.usecase

import android.net.Uri
import com.flixclusive.core.database.entity.downloads.DownloadItem

data class CompletedDownloadFile(
    val uri: Uri,
    val mimeType: String,
)

interface GetCompletedDownloadFileUseCase {
    suspend operator fun invoke(item: DownloadItem): CompletedDownloadFile?
}
