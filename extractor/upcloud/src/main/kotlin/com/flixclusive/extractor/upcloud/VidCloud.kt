package com.flixclusive.extractor.upcloud

import com.flixclusive.core.util.coroutines.asyncCalls
import com.flixclusive.core.util.coroutines.mapAsync
import com.flixclusive.core.util.coroutines.mapIndexedAsync
import com.flixclusive.core.util.network.CryptographyUtil.decryptAes
import com.flixclusive.core.util.network.fromJson
import com.flixclusive.core.util.network.request
import com.flixclusive.extractor.upcloud.dto.DecryptedSource
import com.flixclusive.extractor.upcloud.dto.UpCloudEmbedData
import com.flixclusive.extractor.upcloud.dto.UpCloudEmbedData.Companion.toSubtitle
import com.flixclusive.extractor.upcloud.dto.VidCloudEmbedDataCustomDeserializer
import com.flixclusive.model.provider.SourceLink
import com.flixclusive.model.provider.Subtitle
import com.flixclusive.provider.base.extractor.Extractor
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

        val key = "NzEKyzYxAg=="
        val browserVersion = "1878522624"
        val kId = "ea48b9befe5b27756bf749a11537bf64ea51f2db"
        val kVersion = "12542"

        val sourceEndpoint = "$hostToUse/ajax/v2/embed-4/getSources?id=$id&v=${kVersion}&h=${kId}&b=${browserVersion}"
        val response = client.request(
            url = sourceEndpoint,
            headers = options
        ).execute()

        val responseBody = response.body
            ?.string()
            ?: throw Exception("Cannot fetch source")

        if(responseBody.isBlank())
            throw Exception("Cannot fetch source")

        val upCloudEmbedData = fromJson<UpCloudEmbedData>(
            json = responseBody,
            serializer = VidCloudEmbedDataCustomDeserializer {
                fromJson<List<DecryptedSource>>(
                    decryptAes(it, key)
                )
            }
        )

        upCloudEmbedData.run {
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
                        client.request(
                            url = source.url,
                            headers = options
                        ).execute().body
                            ?.string()
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
}