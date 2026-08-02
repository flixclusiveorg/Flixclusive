package com.flixclusive.domain.downloads.usecase

import com.flixclusive.core.common.domain.Async
import com.flixclusive.model.provider.link.Stream

/**
 * @param primary the stream to download first.
 * @param fallbacks the remaining ranked candidates, in fallback order, to try if [primary]
 * (or a later fallback) fails partway through the download.
 */
data class ResolvedDownloadableStream(
    val primary: Stream,
    val fallbacks: List<Stream>,
)

interface ResolveDownloadableStreamUseCase {
    suspend operator fun invoke(streams: List<Stream>): Async<ResolvedDownloadableStream>
}
