package com.flixclusive.domain.downloads.usecase

import com.flixclusive.model.media.MediaMetadata
import com.flixclusive.model.media.common.tv.Episode
import com.hippo.unifile.UniFile

interface GetDownloadDirectoryUseCase {
    suspend operator fun invoke(
        media: MediaMetadata,
        episode: Episode? = null,
    ): UniFile?
}
