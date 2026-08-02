package com.flixclusive.domain.downloads.usecase

import com.flixclusive.core.common.domain.Async
import com.flixclusive.model.provider.link.Stream

interface ResolveDownloadableStreamUseCase {
    suspend operator fun invoke(streams: List<Stream>): Async<Stream>
}
