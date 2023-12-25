package com.flixclusive.providers.extractors.mixdrop

import com.flixclusive.providers.extractors.mixdrop.utils.Unpacker
import com.flixclusive.providers.interfaces.Extractor
import com.flixclusive.providers.models.common.EmbedData
import com.flixclusive.providers.utils.network.OkHttpUtils.GET
import com.flixclusive.providers.utils.network.OkHttpUtils.asJsoup
import com.flixclusive.providers.utils.network.OkHttpUtils.asString
import okhttp3.Headers
import okhttp3.OkHttpClient
import java.net.URL

class MixDrop(
    private val client: OkHttpClient,
) : Extractor {
    override val name: String = "mixdrop"
    private val oldHost = "mixdrop.co"
    private val newHost = "mixdrop.vc"

    override suspend fun extract(
        url: URL,
        mediaId: String,
        episodeId: String,
        isAlternative: Boolean,
    ): EmbedData {
        val newUrl = URL(url.toString().replace(oldHost, newHost))
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