package com.flixclusive.data.tmdb.repository.impl

import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.network.util.Resource
import com.flixclusive.core.util.exception.actualMessage
import com.flixclusive.core.util.log.errorLog
import com.flixclusive.core.util.network.jsoup.asJsoup
import com.flixclusive.core.util.network.okhttp.request
import com.flixclusive.data.tmdb.repository.TMDBWatchProvidersRepository
import com.flixclusive.model.provider.link.Flag
import com.flixclusive.model.provider.link.Stream
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.jsoup.nodes.Document
import retrofit2.HttpException
import java.net.URLDecoder
import javax.inject.Inject

internal class TMDBWatchProvidersRepositoryImpl
    @Inject
    constructor(
        private val okHttpClient: OkHttpClient,
        private val appDispatchers: AppDispatchers,
    ) : TMDBWatchProvidersRepository {
        override suspend fun getWatchProviders(
            mediaType: String,
            id: Int,
        ): Resource<List<Stream>> {
            require(mediaType == "movie" || mediaType == "tv") {
                "Invalid media type: $mediaType"
            }

            return try {
                val response = withContext(appDispatchers.io) {
                    okHttpClient
                        .request(
                            url = "https://www.themoviedb.org/$mediaType/$id/watch?locale=US",
                        ).execute()
                }

                withContext(appDispatchers.default) {
                    val html = response.asJsoup()
                    val streams = parseStreamingInfo(html)
                    Resource.Success(streams)
                }
            } catch (e: HttpException) {
                errorLog("Http Error (${e.code()}): ${e.response()}")
                errorLog(e)
                Resource.Failure(e.actualMessage)
            } catch (e: Exception) {
                errorLog(e)
                Resource.Failure(e.actualMessage)
            }
        }

        private fun parseStreamingInfo(html: Document): List<Stream> {
            val streamingInfoList = mutableListOf<Stream>()

            html.select("div.ott_provider li a").forEach { element ->
                val href = element.attr("href")
                val title = element.attr("title")
                val logoUrl = element.select("img").attr("src")

                val providerName = title
                    .split(" on ")
                    .lastOrNull()
                    ?.trim()
                    ?: "Unknown Provider"

                // Extract the URL from the 'r' parameter in the href
                val url = href
                    .split("&r=")
                    .getOrNull(1)
                    ?.split("&")
                    ?.firstOrNull()
                    ?.let { URLDecoder.decode(it, "UTF-8") }

                if (url != null && !streamingInfoList.any { it.url == url }) {
                    streamingInfoList.add(
                        Stream(
                            name = providerName,
                            description = title,
                            url = url,
                            flags = setOf(
                                Flag.Trusted(
                                    name = providerName,
                                    logo = logoUrl,
                                ),
                            ),
                        ),
                    )
                }
            }

            return streamingInfoList
        }
    }
