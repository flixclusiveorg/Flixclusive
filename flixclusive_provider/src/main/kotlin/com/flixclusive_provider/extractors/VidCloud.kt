package com.flixclusive_provider.extractors

import com.flixclusive_provider.interfaces.Extractor
import com.flixclusive_provider.models.common.Subtitle
import com.flixclusive_provider.models.common.VideoData
import com.flixclusive_provider.models.extractors.vidcloud.DecryptedSource
import com.flixclusive_provider.models.extractors.vidcloud.VidCloudEmbedData
import com.flixclusive_provider.utils.Constants.USER_AGENT
import com.flixclusive_provider.utils.DecryptUtils.decryptAes
import com.flixclusive_provider.utils.DecryptUtils.extractEmbedDecryptionDetails
import com.flixclusive_provider.utils.JsonUtils.fromJson
import com.flixclusive_provider.utils.OkHttpUtils.asString
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.URL

class VidCloud(
    private val client: OkHttpClient
) : Extractor {
    private val host: String = "https://dokicloud.one"
    private val alternateHost: String = "https://rabbitstream.net"

    override fun extract(
        url: URL,
        mediaId: String,
        episodeId: String,
        isAlternative: Boolean
    ): VideoData {
        try {
            val id = url.path.split('/').last().split('?').first()
            val options = Headers.Builder()
                .add("X-Requested-With", "XMLHttpRequest")
                .add("Referer", url.toString())
                .add("User-Agent", USER_AGENT)
                .build()

            val hostToUse = if (isAlternative) alternateHost else host
            val response = client.newCall(
                Request.Builder()
                    .url("$hostToUse/ajax/embed-4/getSources?id=$id")
                    .headers(options)
                    .build()
            ).execute().body?.charStream().asString() ?: throw Error("Could not fetch sources.")

            val embedData = fromJson<VidCloudEmbedData>(response)
            var source = embedData.sources

            if (embedData.encrypted) {
                var key = client.newCall(
                    Request.Builder()
                        .url("https://raw.githubusercontent.com/enimax-anime/key/e4/key.txt")
                        .headers(options)
                        .build()
                ).execute().body?.charStream().asString() ?: throw Error("Could not fetch key.")

                if(key.contains("[")) {
                    val stops = fromJson<List<List<Int>>>(key)
                    val (decryptedKey, newSource) = extractEmbedDecryptionDetails(embedData.sources, stops)

                    key = decryptedKey
                    source = newSource
                }

                source = fromJson<List<DecryptedSource>>(decryptAes(source, key))[0].url
            }

            return VideoData(
                mediaId = mediaId,
                episodeId = episodeId,
                source = source,
                subtitles = embedData.tracks.map {
                    Subtitle(
                        url = it.url,
                        lang = it.lang
                    )
                }
            )
        } catch (e: IOException) {
            throw Error(e.message)
        }
    }
}