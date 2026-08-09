package com.flixclusive.domain.downloads.usecase

import com.flixclusive.core.common.domain.Async
import com.flixclusive.model.provider.link.Stream

/**
 * @param isHls whether [stream] is an HLS manifest rather than a direct file.
 * @param contentLength the probed byte size of [stream], if known. Only meaningful for direct
 * files — HLS totals are tracked as segment counts instead, set once the manifest is resolved.
 */
data class RankedDownloadCandidate(
    val stream: Stream,
    val isHls: Boolean,
    val contentLength: Long? = null,
)

interface ResolveDownloadableStreamUseCase {
    suspend operator fun invoke(
        ownerId: String,
        mediaId: String,
        seasonNumber: Int?,
        episodeNumber: Int?,
    ): Async<RankedDownloadCandidate>
}
