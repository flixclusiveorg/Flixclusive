package com.flixclusive.domain.downloads.usecase

import com.flixclusive.core.common.domain.Async
import com.flixclusive.model.provider.link.Stream

/** @param isHls whether [stream] is an HLS manifest rather than a direct file. */
data class RankedDownloadCandidate(
    val stream: Stream,
    val isHls: Boolean,
)

interface ResolveDownloadableStreamUseCase {
    suspend operator fun invoke(
        ownerId: String,
        mediaId: String,
        seasonNumber: Int?,
        episodeNumber: Int?,
    ): Async<RankedDownloadCandidate>
}
