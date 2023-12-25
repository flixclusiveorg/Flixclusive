package com.flixclusive.providers.extractors.upcloud

import com.flixclusive.providers.extractors.upcloud.utils.DecryptUtils.extractEmbedDecryptionDetails
import com.flixclusive.providers.extractors.upcloud.utils.DecryptUtils.getKeyStops
import com.flixclusive.providers.interfaces.Extractor
import com.flixclusive.providers.models.common.EmbedData
import com.flixclusive.providers.models.common.VideoDataServer
import com.flixclusive.providers.models.extractors.upcloud.DecryptedSource
import com.flixclusive.providers.models.extractors.upcloud.UpCloudEmbedData
import com.flixclusive.providers.models.extractors.upcloud.UpCloudEmbedData.Companion.toSubtitle
import com.flixclusive.providers.utils.DecryptUtils.decryptAes
import com.flixclusive.providers.utils.JsonUtils.fromJson
import com.flixclusive.providers.utils.network.OkHttpUtils.GET
import com.flixclusive.providers.utils.network.OkHttpUtils.asString
import okhttp3.Headers
import okhttp3.OkHttpClient
import java.net.URL

/**
 *
 * Also known as vidcloud
 * */
class UpCloud(
    private val client: OkHttpClient,
) : Extractor {
    override val name: String = "upcloud"
    private val alternativeName: String = "vidcloud"

    private val host: String = "https://rabbitstream.net"
    private val alternateHost: String = "https://dokicloud.one"
    private val e4ScriptEndpoint = "https://rabbitstream.net/js/player/prod/e4-player.min.js"

    private fun getName(isAlternative: Boolean) = if(isAlternative) alternativeName else name

    override suspend fun extract(
        url: URL,
        mediaId: String,
        episodeId: String,
        isAlternative: Boolean,
    ): EmbedData? {
        val servers = arrayListOf<VideoDataServer>()

        val id = url.path.split('/').last().split('?').first()
        val options = Headers.Builder()
            .add("X-Requested-With", "XMLHttpRequest")
            .add("Referer", url.toString())
            .build()

        val hostToUse = if (isAlternative) alternateHost else host
        val response = client.newCall(
            GET("$hostToUse/ajax/embed-4/getSources?id=$id", options)
        ).execute()

        val responseBody = response.body?.charStream().asString() ?: return null


        if(responseBody.isBlank())
            return null

        val upCloudEmbedData: UpCloudEmbedData

        try {
            upCloudEmbedData = fromJson<UpCloudEmbedData>(responseBody)
        } catch (e: Exception) {
            println("!! Source could be an array !!")
            println(responseBody)

            return null
        }

        var sources = mutableListOf<DecryptedSource>()

        if (upCloudEmbedData.encrypted) {
            val e4Script = client.newCall(
                GET(e4ScriptEndpoint, options)
            ).execute().body?.charStream().asString()
                ?: return null

            val stops = getKeyStops(e4Script)
            val (decryptedKey, newSource) = extractEmbedDecryptionDetails(upCloudEmbedData.sources, stops)

            sources = fromJson<MutableList<DecryptedSource>>(decryptAes(newSource, decryptedKey))
        }

        sources.forEach { source ->
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

                    val extractedServers = qualities.mapIndexed { i, s ->
                        val qualityTag = "${getName(isAlternative)}: ${s.split('x')[1]}p"
                        val dataUrl = urls[i]
                        dataUrl to qualityTag
                    }

                    for ((dataUrl, qualityTag) in extractedServers) {
                        servers.add(
                            VideoDataServer(
                                serverName = qualityTag,
                                serverUrl = dataUrl
                            )
                        )
                    }
                }
        }

        try {
            check(sources.isNotEmpty())
            servers.add(
                index = 0,
                element = VideoDataServer(
                    serverUrl = sources[0].url,
                    serverName = "${getName(isAlternative)}: " + "Auto"
                )
            )
        } catch (_: Exception) { }

        return EmbedData(
            servers = servers,
            subtitles = upCloudEmbedData.tracks.map {
                it.toSubtitle(
                    customName = "${getName(isAlternative)}: ",
                )
            }
        )
    }
}