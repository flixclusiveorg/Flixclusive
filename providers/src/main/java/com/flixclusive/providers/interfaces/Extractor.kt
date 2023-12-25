package com.flixclusive.providers.interfaces

import com.flixclusive.providers.models.common.EmbedData
import com.flixclusive.providers.models.common.VideoDataServer
import java.net.URL

interface Extractor {
    val name: String

    suspend fun extract(
        url: URL,
        mediaId: String,
        episodeId: String,
        isAlternative: Boolean = false
    ): EmbedData?
}