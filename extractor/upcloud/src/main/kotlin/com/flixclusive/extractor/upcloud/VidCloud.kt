package com.flixclusive.extractor.upcloud

import com.flixclusive.core.util.coroutines.asyncCalls
import com.flixclusive.core.util.coroutines.mapAsync
import com.flixclusive.core.util.coroutines.mapIndexedAsync
import com.flixclusive.core.util.json.fromJson
import com.flixclusive.core.util.log.debugLog
import com.flixclusive.core.util.network.CryptographyUtil.decryptAes
import com.flixclusive.core.util.network.GET
import com.flixclusive.core.util.network.asString
import com.flixclusive.extractor.base.Extractor
import com.flixclusive.extractor.upcloud.dto.DecryptedSource
import com.flixclusive.extractor.upcloud.dto.UpCloudEmbedData
import com.flixclusive.extractor.upcloud.dto.UpCloudEmbedData.Companion.toSubtitle
import com.flixclusive.extractor.upcloud.util.DecryptUtils.extractEmbedDecryptionDetails
import com.flixclusive.extractor.upcloud.util.DecryptUtils.getKeyStops
import com.flixclusive.model.provider.SourceLink
import com.flixclusive.model.provider.Subtitle
import okhttp3.Headers
import okhttp3.OkHttpClient
import java.net.URL

/**
 *
 * Also known as vidcloud
 * */
class VidCloud(
    private val client: OkHttpClient,
    private val isAlternative: Boolean = false,
) : Extractor() {
    override val name: String = "vidcloud"
    override val alternateNames: List<String>
        get() = listOf("upcloud")

    override val host: String = "https://rabbitstream.net"
    private val alternateHost: String = "https://dokicloud.one"
    private val e4ScriptEndpoint = "https://rabbitstream.net/js/player/prod/e4-player.min.js"

    private fun getHost(isAlternative: Boolean) =
        (if (isAlternative) "DokiCloud" else "Rabbitstream")

    override suspend fun extract(
        url: URL,
        mediaId: String,
        episodeId: String,
        onLinkLoaded: (SourceLink) -> Unit,
        onSubtitleLoaded: (Subtitle) -> Unit,
    ) {
        val id = url.path.split('/').last().split('?').first()
        val options = Headers.Builder()
            .add("X-Requested-With", "XMLHttpRequest")
            .add("Referer", url.toString())
            .build()

        val hostToUse = if (isAlternative) alternateHost else host
        val response = client.newCall(
            GET("$hostToUse/ajax/embed-4/getSources?id=$id", options)
        ).execute()

        val responseBody = response.body
            ?.charStream()
            .asString()
            ?: throw Exception("Cannot fetch sources")


        if(responseBody.isBlank())
            throw Exception("Cannot fetch sources")

        val upCloudEmbedData: UpCloudEmbedData

        try {
            upCloudEmbedData = fromJson<UpCloudEmbedData>(responseBody)
        } catch (e: Exception) {
            debugLog("!! Source could be an array !!")
            debugLog(responseBody)

            throw Exception("Invalid source type. Possibly an array")
        }

        var sources = mutableListOf<DecryptedSource>()

        if (upCloudEmbedData.encrypted) {
            val e4Script = client.newCall(
                GET(e4ScriptEndpoint, options)
            ).execute().body?.charStream().asString()
                ?: throw Exception("Cannot fetch key decoder")

            val stops = getKeyStops(e4Script)
            val (decryptedKey, newSource) = extractEmbedDecryptionDetails(upCloudEmbedData.sources, stops)

            sources = fromJson<MutableList<DecryptedSource>>(
                decryptAes(newSource, decryptedKey)
            )
        }


        check(sources.isNotEmpty())
        onLinkLoaded(
            SourceLink(
                url = sources[0].url,
                name = "${getHost(isAlternative)}: " + "Auto"
            )
        )

        asyncCalls(
            {
                sources.mapAsync { source ->
                    client.newCall(
                        GET(source.url, options)
                    ).execute().body
                        ?.charStream()
                        .asString()
                        ?.let { data ->
                            val urls = data
                                .split('\n')
                                .filter { line -> line.contains(".m3u8") }

                            val qualities = data
                                .split('\n')
                                .filter { line -> line.contains("RESOLUTION=") }

                            qualities.mapIndexedAsync { i, s ->
                                val qualityTag = "${getHost(isAlternative)}: ${s.split('x')[1]}p"
                                val dataUrl = urls[i]

                                onLinkLoaded(
                                    SourceLink(
                                        name = qualityTag,
                                        url = dataUrl
                                    )
                                )
                            }
                        }
                }
            },
            {
                upCloudEmbedData.tracks.mapAsync {
                    onSubtitleLoaded(it.toSubtitle())
                }
            }
        )
    }
}