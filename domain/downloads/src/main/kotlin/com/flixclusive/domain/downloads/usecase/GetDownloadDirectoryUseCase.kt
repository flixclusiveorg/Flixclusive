package com.flixclusive.domain.downloads.usecase

import com.hippo.unifile.UniFile

interface GetDownloadDirectoryUseCase {
    suspend operator fun invoke(
        mediaId: String,
        mediaTitle: String,
        seasonNumber: Int? = null,
        episodeNumber: Int? = null,
    ): UniFile?
}
