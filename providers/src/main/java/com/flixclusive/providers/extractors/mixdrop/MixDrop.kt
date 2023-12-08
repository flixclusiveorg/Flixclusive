package com.flixclusive.providers.extractors.mixdrop

import com.flixclusive.providers.extractors.mixdrop.utils.Unpacker
import com.flixclusive.providers.interfaces.Extractor
import com.flixclusive.providers.models.common.VideoData
import com.flixclusive.providers.utils.network.OkHttpUtils.GET
import com.flixclusive.providers.utils.network.OkHttpUtils.asJsoup
import okhttp3.Headers
import okhttp3.OkHttpClient
import java.net.URL

class MixDrop(
    private val client: OkHttpClient
) : Extractor {
    private val oldHost = "mixdrop.co"
    private val newHost = "mixdrop.ag"

    @Suppress("UNREACHABLE_CODE")
    override suspend fun extract(
        url: URL,
        mediaId: String,
        episodeId: String,
        isAlternative: Boolean,
    ): VideoData {
        throw Exception("Not available")

        val newUrl = URL(url.toString().replace(oldHost, newHost))
        val headers = Headers.headersOf("Referer", newUrl.toString())
        val response = client.newCall(
            GET(newUrl.toString(), headers)
        ).execute().use { it.asJsoup() }

        val packed = response.selectFirst("script:containsData(eval)")?.data()

        println(packed)
        val unpacked = packed?.let(Unpacker::unpack)
            ?: throw Exception("Could not fetch sources.")

        val videoUrl = "https:" + unpacked.substringAfter("Core.wurl=\"")
            .substringBefore("\"")

        val subs = unpacked.substringAfter("Core.remotesub=\"").substringBefore('"')

        return VideoData(
            mediaId = mediaId,
            source = videoUrl,
            subtitles = emptyList()
        )
    }
}