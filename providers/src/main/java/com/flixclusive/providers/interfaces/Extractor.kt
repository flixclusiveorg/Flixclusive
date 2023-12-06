package com.flixclusive.providers.interfaces

import com.flixclusive.providers.models.common.VideoData
import java.net.URL

interface Extractor {
    suspend fun extract(
        url: URL,
        mediaId: String,
        episodeId: String,
        isAlternative: Boolean = false
    ): VideoData
}