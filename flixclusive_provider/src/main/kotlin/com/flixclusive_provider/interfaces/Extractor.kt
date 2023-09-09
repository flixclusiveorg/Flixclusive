package com.flixclusive_provider.interfaces

import com.flixclusive_provider.models.common.VideoData
import java.net.URL

interface Extractor {
    fun extract(
        url: URL,
        mediaId: String,
        episodeId: String,
        isAlternative: Boolean = false
    ): VideoData
}