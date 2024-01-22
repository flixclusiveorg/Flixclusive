package com.flixclusive.extractor.mixdrop

import com.flixclusive.core.util.network.GET
import com.flixclusive.core.util.network.asJsoup
import com.flixclusive.core.util.network.asString
import com.flixclusive.extractor.base.Extractor
import com.flixclusive.extractor.mixdrop.util.Unpacker
import com.flixclusive.model.provider.SourceLink
import com.flixclusive.model.provider.Subtitle
import okhttp3.Headers
import okhttp3.OkHttpClient
import java.net.URL

class MixDrop(
    private val client: OkHttpClient,
) : Extractor() {
    override val name: String = "mixdrop"
    override val host: String = "mixdrop.co"
    private val newHost = "mixdrop.vc"

    override suspend fun extract(
        url: URL,
        mediaId: String,
        episodeId: String,
        onLinkLoaded: (SourceLink) -> Unit,
        onSubtitleLoaded: (Subtitle) -> Unit,
    ) {
        val newUrl = URL(url.toString().replace(host, newHost))
        val headers = Headers.headersOf("Referer", newUrl.toString())
        val response = client.newCall(
            GET(newUrl.toString(), headers)
        ).execute().use {
            if(it.body?.charStream().asString()?.contains("WE ARE SORRY", true) == true)
                throw NullPointerException("Mixdrop cannot find the source.")

            it.asJsoup()
        }

        val packed = response.selectFirst("script:containsData(eval)")?.data()

        val unpacked = packed?.let(Unpacker::unpack)
            ?: throw Exception("Could not fetch sources.")

        val videoUrl = "https:" + unpacked.substringAfter("Core.wurl=\"")
            .substringBefore("\"")

        val subs = unpacked.substringAfter("Core.remotesub=\"").substringBefore('"')

        TODO("Return the embed data from MIXDROP")
    }
}