package com.flixclusive.providers.extractors.upcloud

import com.flixclusive.providers.extractors.upcloud.utils.DecryptUtils.extractEmbedDecryptionDetails
import com.flixclusive.providers.extractors.upcloud.utils.DecryptUtils.getKeyStops
import com.flixclusive.providers.interfaces.Extractor
import com.flixclusive.providers.models.common.Subtitle
import com.flixclusive.providers.models.common.VideoData
import com.flixclusive.providers.models.extractors.vidcloud.DecryptedSource
import com.flixclusive.providers.models.extractors.vidcloud.VidCloudEmbedData
import com.flixclusive.providers.utils.DecryptUtils.decryptAes
import com.flixclusive.providers.utils.JsonUtils.fromJson
import com.flixclusive.providers.utils.OkHttpUtils.GET
import com.flixclusive.providers.utils.OkHttpUtils.asString
import okhttp3.Headers
import okhttp3.OkHttpClient
import java.net.URL

class UpCloud(
    private val client: OkHttpClient,
) : Extractor {
    private val host: String = "https://rabbitstream.net"
    private val alternateHost: String = "https://dokicloud.one"
    private val e4ScriptEndpoint = "https://rabbitstream.net/js/player/prod/e4-player.min.js"

    override suspend fun extract(
        url: URL,
        mediaId: String,
        episodeId: String,
        isAlternative: Boolean,
    ): VideoData {
        val id = url.path.split('/').last().split('?').first()
        val options = Headers.Builder()
            .add("X-Requested-With", "XMLHttpRequest")
            .add("Referer", url.toString())
            .build()

        val hostToUse = if (isAlternative) alternateHost else host
        val response = client.newCall(
            GET("$hostToUse/ajax/embed-4/getSources?id=$id", options)
        ).execute()

        val responseBody = response.body?.charStream().asString() ?: throw Exception("Could not fetch sources.")

        if(responseBody.isBlank())
            throw Exception("Server might be down.")

        val embedData = fromJson<VidCloudEmbedData>(responseBody)
        var source = embedData.sources

        if (embedData.encrypted) {
            val e4Script = client.newCall(
                GET(e4ScriptEndpoint, options)
            ).execute().body?.charStream().asString()
                ?: throw Exception("Could not fetch e4 script.")

            val stops = getKeyStops(e4Script)
            val (decryptedKey, newSource) = extractEmbedDecryptionDetails(embedData.sources, stops)

            source = fromJson<List<DecryptedSource>>(decryptAes(newSource, decryptedKey))[0].url
        }

        return VideoData(
            mediaId = mediaId,
            source = source,
            subtitles = embedData.tracks.map {
                Subtitle(
                    url = it.url,
                    lang = it.lang
                )
            }
        )
    }
}