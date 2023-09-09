package com.flixclusive_provider.extractors

import com.flixclusive_provider.interfaces.Extractor
import com.flixclusive_provider.models.common.VideoData
import okhttp3.OkHttpClient
import java.net.URL

class MixDrop(
    private val client: OkHttpClient
) : Extractor {

    override fun extract(
        url: URL,
        mediaId: String,
        episodeId: String,
        isAlternative: Boolean,
    ): VideoData {
        throw IllegalStateException("MixDrop is currently unavailable!")
    }
}