package com.flixclusive.domain.downloads.usecase

import com.flixclusive.core.common.domain.Async
import com.flixclusive.model.provider.link.Stream

/** @param isHls whether [stream] is an HLS manifest rather than a direct file. */
data class RankedDownloadCandidate(
    val stream: Stream,
    val isHls: Boolean,
)

/**
 * @param primary the stream to download first.
 * @param fallbacks the remaining ranked candidates, in fallback order, to try if [primary]
 * (or a later fallback) fails partway through the download.
 */
data class ResolvedDownloadableStream(
    val primary: RankedDownloadCandidate,
    val fallbacks: List<RankedDownloadCandidate>,
)

interface ResolveDownloadableStreamUseCase {
    suspend operator fun invoke(streams: List<Stream>): Async<ResolvedDownloadableStream>
}
