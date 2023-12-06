package com.flixclusive.providers.sources.flixhq

import com.flixclusive.providers.extractors.mixdrop.MixDrop
import com.flixclusive.providers.extractors.upcloud.UpCloud
import com.flixclusive.providers.flixhq.utils.FlixHQUtils.getEpisodeId
import com.flixclusive.providers.flixhq.utils.FlixHQUtils.getSeasonId
import com.flixclusive.providers.flixhq.utils.FlixHQUtils.getServerName
import com.flixclusive.providers.flixhq.utils.FlixHQUtils.getServerUrl
import com.flixclusive.providers.flixhq.utils.FlixHQUtils.toSearchResultItem
import com.flixclusive.providers.interfaces.Server
import com.flixclusive.providers.interfaces.SourceProvider
import com.flixclusive.providers.models.common.MediaInfo
import com.flixclusive.providers.models.common.MediaType
import com.flixclusive.providers.models.common.SearchResultItem
import com.flixclusive.providers.models.common.SearchResults
import com.flixclusive.providers.models.common.VideoData
import com.flixclusive.providers.models.common.VideoDataServer
import com.flixclusive.providers.models.providers.flixhq.FlixHQInitialSourceData
import com.flixclusive.providers.models.providers.flixhq.TvShowCacheData
import com.flixclusive.providers.utils.JsonUtils.fromJson
import com.flixclusive.providers.utils.OkHttpUtils.GET
import com.flixclusive.providers.utils.OkHttpUtils.asString
import okhttp3.OkHttpClient
import org.jsoup.Jsoup
import java.net.URL
import java.net.URLDecoder
import java.util.*

@Suppress("SpellCheckingInspection")
class FlixHQ(
    private val client: OkHttpClient,
    private val upCloudExtractor: UpCloud = UpCloud(client),
    private val mixDropExtractor: MixDrop = MixDrop(client),
) : SourceProvider() {
    override val name: String = "FlixHQ"
    override val baseUrl: String = "https://flixhq.to"

    /**
     *
     * Cached data of previous called tv show.
     * This will ease rapid (tv) episodes requests from
     * the source/provider.
     *
     * */
    private var tvCacheData: TvShowCacheData = TvShowCacheData()

    override val providerServers: List<Server> = listOf(
        "upcloud",
        "vidcloud",
    )

    override suspend fun search(
        query: String,
        page: Int,
    ): SearchResults {
        var searchResult = SearchResults(page, false, listOf())

        val response = client.newCall(
            GET(
                "${baseUrl}/search/${
                    query.replace(
                        Regex("[\\W_]+"),
                        "-"
                    )
                }?page=$page"
            )
        ).execute()

        response.body?.charStream().asString()?.let { data ->
            val doc = Jsoup.parse(data)
            val navSelector = "div.pre-pagination:nth-child(3) > nav:nth-child(1) > ul:nth-child(1)"
            searchResult = searchResult.copy(
                hasNextPage = doc.select(navSelector).size > 0 && doc.select(navSelector).last()
                    ?.hasClass("active") == false
            )

            val results = mutableListOf<SearchResultItem>()

            doc.select(".film_list-wrap > div.flw-item").forEach { element ->
                results.add(element.toSearchResultItem(baseUrl))
            }

            return searchResult.copy(results = results)
        }

        return SearchResults()
    }

    override suspend fun getMediaInfo(
        mediaId: String,
        mediaType: MediaType,
    ): MediaInfo {
        var updatedMediaId = mediaId
        if (!mediaId.startsWith(baseUrl)) {
            updatedMediaId = "$baseUrl/$mediaId"
        }

        if (tvCacheData.mediaInfo?.id == updatedMediaId)
            return tvCacheData.mediaInfo!!

        var mediaInfo = MediaInfo(
            id = updatedMediaId.split("to/").last(),
            title = ""
        )

        val response = client.newCall(GET(updatedMediaId)).execute()
        val data = response.body?.charStream().asString()

        if (data != null) {
            val doc = Jsoup.parse(data)
            mediaInfo = mediaInfo.copy(
                title = doc.select(".heading-name > a:nth-child(1)").text()
            )

            val uid = doc.select(".watch_block").attr("data-id")
            val releaseDate = Jsoup.parse(data).select("div.row-line:nth-child(3)").text()
                .replace("Released: ", "").trim()
            mediaInfo = mediaInfo.copy(releaseDate = releaseDate)

            if (mediaType == MediaType.Movie) {
                mediaInfo = mediaInfo.copy(
                    id = uid,
                    title = "${mediaInfo.title} Movie",
                )
            }

            tvCacheData = TvShowCacheData(id = uid, mediaInfo)
            return mediaInfo
        }

        throw NullPointerException("MediaInfo is null!")
    }

    private fun getEpisodeId(
        mediaId: String,
        episode: Int,
        season: Int,
    ): String {
        val updatedMediaId = mediaId.split("-").last()
        val isSameId = tvCacheData.id == updatedMediaId

        val ajaxReqUrl: (String, String, Boolean) -> String = { id, type, isSeasons ->
            "$baseUrl/ajax/v2/$type/${if (isSeasons) "seasons" else "episodes"}/$id"
        }

        if (isSameId) {
            tvCacheData.episodes
                ?.getEpisodeId(episode)
                ?.let {
                    return it
                }
        }

        if (tvCacheData.seasons == null || !isSameId) {
            val responseSeasons =
                client.newCall(GET(ajaxReqUrl(updatedMediaId, "tv", true))).execute()
            val dataSeasons = responseSeasons.body?.charStream().asString()
                ?: throw Exception("Failed to fetch season data from provider")

            val seasonsDoc = Jsoup.parse(dataSeasons)
                .select(".dropdown-menu > a")

            tvCacheData = if (isSameId) {
                tvCacheData.copy(seasons = seasonsDoc)
            } else {
                TvShowCacheData(id = updatedMediaId, seasons = seasonsDoc)
            }
        }

        val seasons = tvCacheData.seasons!!

        if (seasons.size < season)
            throw Exception("Season $season is not available")

        val seasonId =
            seasons.getSeasonId(season) ?: throw Exception("Season $season is not available")

        val responseEpisodes = client.newCall(GET(ajaxReqUrl(seasonId, "season", false))).execute()
        val dataEpisodes = responseEpisodes.body?.string()
            ?: throw Exception("Failed to fetch episode id from provider")

        val docEpisodes = Jsoup.parse(dataEpisodes)
        val episodes = docEpisodes.select(".nav > li")

        tvCacheData = if (isSameId) {
            tvCacheData.copy(episodes = episodes)
        } else {
            TvShowCacheData(
                id = updatedMediaId,
                seasons = seasons,
                episodes = episodes
            )
        }

        return episodes.getEpisodeId(episode) ?: throw Exception("Cannot find episode id!")
    }

    override suspend fun getSourceLinks(
        mediaId: String,
        server: String?,
        season: Int?,
        episode: Int?
    ): VideoData {
        val isTvShow = season != null && episode != null

        val availableServers = getAvailableServers(
            mediaId = mediaId,
            season = season,
            episode = episode
        ).map {
            it.copy(isEmbed = true)
        }

        if (availableServers.isEmpty())
            throw Exception("No available servers.")

        var serverToUse = availableServers[0]

        if(server != null) {
            serverToUse = availableServers.find { it.serverName == server } ?: serverToUse
        }

        val response = client.newCall(
            GET("${baseUrl}/ajax/get_link/${serverToUse.serverUrl.split('.').last()}")
        ).execute()

        response.body
            ?.charStream()
            ?.asString()
            ?.let { initialSourceData ->
                val url = URLDecoder.decode(
                    fromJson<FlixHQInitialSourceData>(initialSourceData).link,
                    "UTF-8"
                )

                val episodeId = if(isTvShow) {
                    getEpisodeId(
                        mediaId = mediaId,
                        episode = episode!!,
                        season = season!!
                    )
                } else mediaId.split("-").last()

                val serverUrl = URL(url)

                extractFromServer(
                    server = serverToUse.serverName,
                    serverUrl = serverUrl,
                    mediaId = mediaId,
                    episodeId = episodeId
                )?.let { data ->
                    return data.copy(
                        servers = availableServers,
                        sourceName = name
                    )
                }

                providerServers.forEach {
                    if(serverToUse.serverName == it)
                        return@forEach

                    val isServerAvailable = availableServers.any { availableServer ->
                        availableServer.serverName == it
                    }

                    if(!isServerAvailable)
                        return@forEach

                    extractFromServer(
                        server = it,
                        serverUrl = serverUrl,
                        mediaId = mediaId,
                        episodeId = episodeId
                    )?.let { data ->
                        return data.copy(
                            servers = availableServers,
                            sourceName = name
                        )
                    }
                }
            }

        throw Exception("Couldn't get video data source.")
    }

    override suspend fun getAvailableServers(
        mediaId: String,
        season: Int?,
        episode: Int?
    ): List<VideoDataServer> {
        val isTvShow = season != null && episode != null

        val episodeId = if(isTvShow) {
            getEpisodeId(
                mediaId = mediaId,
                episode = episode!!,
                season = season!!
            )
        } else mediaId.split("-").last()

        val fetchServerUrl =
            if (!episodeId.startsWith("$baseUrl/ajax") && !mediaId.contains("movie")) {
                "$baseUrl/ajax/v2/episode/servers/$episodeId"
            } else {
                "$baseUrl/ajax/movie/episodes/$episodeId"
            }

        val response = client.newCall(GET(fetchServerUrl)).execute()
        val data = response.body?.charStream().asString()

        if (data != null) {
            val doc = Jsoup.parse(data)
            val servers = doc.select(".nav > li")
                .filter { element ->
                    val serverName = element.select("a").getServerName(mediaId)

                    providerServers.find { it == serverName } != null
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

    private suspend fun extractFromServer(
        server: String,
        serverUrl: URL,
        mediaId: String,
        episodeId: String,
    ): VideoData? {
        return when (server) {
            //"mixdrop" -> mixDropExtractor.extract(
            //    url = serverUrl,
            //    mediaId = mediaId,
            //    episodeId = episodeId
            //)
            "upcloud" -> upCloudExtractor.extract(
                url = serverUrl,
                mediaId = mediaId,
                episodeId = episodeId,
            )

            "vidcloud" -> upCloudExtractor.extract(
                url = serverUrl,
                mediaId = mediaId,
                episodeId = episodeId,
                isAlternative = true
            )

            else -> null
        }
    }
}