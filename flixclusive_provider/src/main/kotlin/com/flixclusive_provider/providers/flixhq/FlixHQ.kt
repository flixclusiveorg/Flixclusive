package com.flixclusive_provider.providers.flixhq

import com.flixclusive_provider.extractors.MixDrop
import com.flixclusive_provider.extractors.VidCloud
import com.flixclusive_provider.interfaces.FilmSourcesProvider
import com.flixclusive_provider.models.common.MediaInfo
import com.flixclusive_provider.models.common.MediaServer
import com.flixclusive_provider.models.common.MediaServer.Companion.toMediaServer
import com.flixclusive_provider.models.common.MediaType
import com.flixclusive_provider.models.common.SearchResultItem
import com.flixclusive_provider.models.common.SearchResults
import com.flixclusive_provider.models.common.VideoData
import com.flixclusive_provider.models.common.VideoDataServer
import com.flixclusive_provider.models.providers.flixhq.FlixHQInitialSourceData
import com.flixclusive_provider.providers.flixhq.utils.ServerUtils.getServerName
import com.flixclusive_provider.providers.flixhq.utils.ServerUtils.getServerUrl
import com.flixclusive_provider.utils.JsonUtils.fromJson
import com.flixclusive_provider.utils.OkHttpUtils.asString
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.net.URL
import java.net.URLDecoder
import java.util.*

@Suppress("SpellCheckingInspection")
class FlixHQ(
    private val client: OkHttpClient,
    private val vidCloudExtractor: VidCloud = VidCloud(client),
    private val mixDropExtractor: MixDrop = MixDrop(client)
) : FilmSourcesProvider {
    override val baseUrl: String = "https://flixhq.to"

    override fun search(
        query: String,
        page: Int,
    ): SearchResults {
        var searchResult = SearchResults(page, false, listOf())

        val request = Request.Builder()
            .url("${baseUrl}/search/${query.replace(Regex("[\\W_]+"), "-")}?page=$page")
            .build()

        val response = client.newCall(request).execute()

        response.body?.charStream().asString()?.let { data ->
            val doc = Jsoup.parse(data)
            val navSelector = "div.pre-pagination:nth-child(3) > nav:nth-child(1) > ul:nth-child(1)"
            searchResult = searchResult.copy(
                hasNextPage = doc.select(navSelector).size > 0 && doc.select(navSelector).last()?.hasClass("active") == false
            )

            val results = mutableListOf<SearchResultItem>()

            doc.select(".film_list-wrap > div.flw-item").forEach { el ->
                val releaseDate = el.select("div.film-detail > div.fd-infor > span:nth-child(1)").text()

                results.add(
                    SearchResultItem(
                        id = el.select("div.film-poster > a").attr("href").substring(1),
                        title = el.select("div.film-detail > h2 > a").attr("title"),
                        url = "${baseUrl}${el.select("div.film-poster > a").attr("href")}",
                        image = el.select("div.film-poster > img").attr("data-src"),
                        releaseDate = if (releaseDate.isNotBlank() && releaseDate.toIntOrNull() != null) releaseDate else null,
                        seasons = if (releaseDate.contains("SS")) releaseDate.split("SS")[1].trim().toIntOrNull() else null,
                        mediaType = if (el.select("div.film-detail > div.fd-infor > span.float-right").text() == "Movie") MediaType.Movie else MediaType.TvShow
                    )
                )
            }

            return searchResult.copy(results = results)
        }

        return SearchResults()
    }

    override fun getMediaInfo(
        mediaId: String,
        mediaType: MediaType,
    ): MediaInfo {
        var updatedMediaId = mediaId
        if (!mediaId.startsWith(baseUrl)) {
            updatedMediaId = "$baseUrl/$mediaId"
        }

        var mediaInfo = MediaInfo(
            id = updatedMediaId.split("to/").last(),
            title = "",
            url = updatedMediaId
        )

        val request = Request.Builder()
            .url(updatedMediaId)
            .build()

        val response = client.newCall(request).execute()
        val data = response.body?.charStream().asString()

        if (data != null) {
            val doc = Jsoup.parse(data)
            mediaInfo = mediaInfo.copy(
                title = doc.select(".heading-name > a:nth-child(1)").text()
            )

            val uid = doc.select(".watch_block").attr("data-id")
            val releaseDate = Jsoup.parse(data).select("div.row-line:nth-child(3)").text().replace("Released: ", "").trim()
            mediaInfo = mediaInfo.copy(releaseDate = releaseDate)

            if (mediaType == MediaType.Movie) {
                mediaInfo = mediaInfo.copy(
                    id = uid,
                    title = "${mediaInfo.title} Movie",
                    url = "$baseUrl/ajax/movie/episodes/$uid"
                )
            }

            return mediaInfo
        }

        throw NullPointerException("MediaInfo is null!")
    }

    override fun getEpisodeId(
        mediaId: String,
        episode: Int,
        season: Int,
    ): String? {
        val ajaxReqUrl: (String, String, Boolean) -> String = { id, type, isSeasons ->
            "$baseUrl/ajax/v2/$type/${if (isSeasons) "seasons" else "episodes"}/$id"
        }

        val id = mediaId.split("-").last()
        val requestSeasons = Request.Builder()
            .url(ajaxReqUrl(id, "tv", true))
            .build()

        val responseSeasons = client.newCall(requestSeasons).execute()
        val dataSeasons = responseSeasons.body?.charStream().asString()

        if (dataSeasons != null) {
            val docSeasons = Jsoup.parse(dataSeasons)
            val seasonId = docSeasons
                .select(".dropdown-menu > a")[season - 1]
                .attr("data-id")

            val requestEpisodes = Request.Builder()
                .url(ajaxReqUrl(seasonId, "season", false))
                .build()
            val responseEpisodes = client.newCall(requestEpisodes).execute()
            val dataEpisodes = responseEpisodes.body?.string()

            if (dataEpisodes != null) {
                val docEpisodes = Jsoup.parse(dataEpisodes)

                val episodes = docEpisodes.select(".nav > li")

                if(episodes.size < episode) {
                    return null
                }

                return episodes[episode - 1]
                    .select("a")
                    .attr("id")
                    .split("-")[1]
            }
        }

        throw IllegalStateException("Episode ID cannot be found!")
    }

    override fun getStreamingLinks(
        episodeId: String,
        mediaId: String,
        initialSourceUrl: String?,
        server: VideoDataServer?
    ): VideoData {
        val serverToUse = server
            ?: getAvailableServers(
                episodeId = episodeId,
                mediaId = mediaId
            )[0]

        if (initialSourceUrl != null) {
            val serverUrl = URL(initialSourceUrl)
            return when (serverToUse.serverName.toMediaServer()) {
                //MediaServer.MixDrop -> mixDropExtractor.extract(
                //    url = serverUrl,
                //    mediaId = mediaId,
                //    episodeId = episodeId
                //)
                MediaServer.VidCloud -> vidCloudExtractor.extract(
                    url = serverUrl,
                    mediaId = mediaId,
                    episodeId = episodeId,
                    isAlternative = true
                )
                MediaServer.UpCloud -> vidCloudExtractor.extract(
                    url = serverUrl,
                    mediaId = mediaId,
                    episodeId = episodeId
                )
            }
        }

        val response = client.newCall(
            Request.Builder()
                .url("${baseUrl}/ajax/get_link/${serverToUse.serverUrl.split('.').last()}")
                .build()
        ).execute()

        val data = response.body?.charStream().asString()

        val serverUrl = data?.let {
            val url = fromJson<FlixHQInitialSourceData>(it).link
            URLDecoder.decode(url, "UTF-8")
        }

        return getStreamingLinks(
            episodeId = episodeId,
            mediaId = mediaId,
            initialSourceUrl = serverUrl,
            server = server
        )
    }

    override fun getAvailableServers(episodeId: String, mediaId: String): List<VideoDataServer> {
        val fetchServerUrl = if (!episodeId.startsWith("$baseUrl/ajax") && !mediaId.contains("movie")) {
            "$baseUrl/ajax/v2/episode/servers/$episodeId"
        } else {
            "$baseUrl/ajax/movie/episodes/$episodeId"
        }

        val request = Request.Builder()
            .url(fetchServerUrl)
            .build()

        val response = client.newCall(request).execute()
        val data = response.body?.charStream().asString()

        if (data != null) {
            val doc = Jsoup.parse(data)
            val servers = doc.select(".nav > li")
                .filter { element ->
                    val serverName = element.select("a").getServerName(mediaId)

                    MediaServer.values().find { it.serverName == serverName } != null
                }
                .map { element ->
                    val anchorElement = element.select("a")

                    VideoDataServer(
                        serverName = anchorElement.getServerName(mediaId),
                        serverUrl = anchorElement.getServerUrl(baseUrl, mediaId)
                    )
                }

            return servers
        }

        return emptyList()
    }
}