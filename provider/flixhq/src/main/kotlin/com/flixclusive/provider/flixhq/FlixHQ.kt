package com.flixclusive.provider.flixhq

import com.flixclusive.core.util.coroutines.mapAsync
import com.flixclusive.core.util.film.FilmType
import com.flixclusive.core.util.json.fromJson
import com.flixclusive.core.util.network.GET
import com.flixclusive.core.util.network.asString
import com.flixclusive.extractor.base.Extractor
import com.flixclusive.extractor.upcloud.UpCloud
import com.flixclusive.model.provider.SourceLink
import com.flixclusive.model.provider.Subtitle
import com.flixclusive.provider.base.Provider
import com.flixclusive.provider.base.dto.FilmInfo
import com.flixclusive.provider.base.dto.SearchResultItem
import com.flixclusive.provider.base.dto.SearchResults
import com.flixclusive.provider.base.util.TvShowCacheData
import com.flixclusive.provider.base.util.replaceWhitespaces
import com.flixclusive.provider.flixhq.dto.FlixHQInitialSourceData
import com.flixclusive.provider.flixhq.util.FlixHQHelper.getEpisodeId
import com.flixclusive.provider.flixhq.util.FlixHQHelper.getSeasonId
import com.flixclusive.provider.flixhq.util.FlixHQHelper.getServerName
import com.flixclusive.provider.flixhq.util.FlixHQHelper.getServerUrl
import com.flixclusive.provider.flixhq.util.FlixHQHelper.toSearchResultItem
import okhttp3.OkHttpClient
import org.jsoup.Jsoup
import java.net.URL
import java.net.URLDecoder

@Suppress("SpellCheckingInspection")
class FlixHQ(client: OkHttpClient) : Provider(client) {
    override val name: String = "FlixHQ"
    override val baseUrl: String = "https://flixhq.to"

    private var tvCacheData: TvShowCacheData = TvShowCacheData()

    override val supportedExtractors: List<Extractor> = listOf(
        UpCloud(client = client),
        UpCloud(client = client, isAlternative = true), // Vidcloud
        //"mixdrop",
    )

    override suspend fun search(
        query: String,
        page: Int,
        filmType: FilmType
    ): SearchResults {
        var searchResult = SearchResults(page, false, listOf())

        val response = client.newCall(
            GET(
                "${baseUrl}/search/${
                    query.replaceWhitespaces("-")
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

    override suspend fun getFilmInfo(
        filmId: String,
        filmType: FilmType
    ): FilmInfo {
        var filmIdToUse = filmId
        if (!filmId.startsWith(baseUrl)) {
            filmIdToUse = "$baseUrl/$filmId"
        }

        if (tvCacheData.filmInfo?.id == filmIdToUse)
            return tvCacheData.filmInfo!!

        var filmInfo = FilmInfo(
            id = filmIdToUse.split("to/").last(),
            title = ""
        )

        val response = client.newCall(GET(filmIdToUse)).execute()
        val data = response.body?.charStream().asString()

        if (data != null) {
            val doc = Jsoup.parse(data)
            filmInfo = filmInfo.copy(
                title = doc.select(".heading-name > a:nth-child(1)").text()
            )

            val uid = doc.select(".watch_block").attr("data-id")
            val releaseDate = Jsoup.parse(data).select("div.row-line:nth-child(3)").text()
                .replace("Released: ", "").trim()
            filmInfo = filmInfo.copy(yearReleased = releaseDate.split("-").first())

            if (filmType == FilmType.MOVIE) {
                filmInfo = filmInfo.copy(
                    id = uid,
                    title = "${filmInfo.title} Movie",
                )
            }

            tvCacheData = TvShowCacheData(id = uid, filmInfo)
            return filmInfo
        }

        throw NullPointerException("FilmInfo is null!")
    }

    private fun getEpisodeId(
        filmId: String,
        episode: Int,
        season: Int,
    ): String {
        val filmIdToUse = filmId.split("-").last()
        val isSameId = tvCacheData.id == filmIdToUse

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
                client.newCall(GET(ajaxReqUrl(filmIdToUse, "tv", true))).execute()
            val dataSeasons = responseSeasons.body?.charStream().asString()
                ?: throw Exception("Failed to fetch season data from provider")

            val seasonsDoc = Jsoup.parse(dataSeasons)
                .select(".dropdown-menu > a")

            tvCacheData = if (isSameId) {
                tvCacheData.copy(seasons = seasonsDoc)
            } else {
                TvShowCacheData(id = filmIdToUse, seasons = seasonsDoc)
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
                id = filmIdToUse,
                seasons = seasons,
                episodes = episodes
            )
        }

        return episodes.getEpisodeId(episode) ?: throw Exception("Cannot find episode id!")
    }

    override suspend fun getSourceLinks(
        filmId: String,
        season: Int?,
        episode: Int?,
        onLinkLoaded: (SourceLink) -> Unit,
        onSubtitleLoaded: (Subtitle) -> Unit,
    ) {
        val isTvShow = season != null && episode != null

        val episodeId = if(isTvShow) {
            getEpisodeId(
                filmId = filmId,
                episode = episode!!,
                season = season!!
            )
        } else filmId.split("-").last()

        val fetchServerUrl =
            if (!episodeId.startsWith("$baseUrl/ajax") && !filmId.contains("movie")) {
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
                    val serverName = element.select("a").getServerName(filmId)

                    supportedExtractors.find { it.name == serverName } != null
                }
                .mapAsync { element ->
                    val anchorElement = element.select("a")

                    SourceLink(
                        name = anchorElement.getServerName(filmId),
                        url = anchorElement.getServerUrl(baseUrl, filmId)
                    )
                }

            servers.mapAsync { server ->
                val serverResponse = client.newCall(
                    GET("${baseUrl}/ajax/get_link/${server.url.split('.').last()}")
                ).execute()

                serverResponse.body
                    ?.charStream()
                    ?.asString()
                    ?.let { initialSourceData ->
                        val serverUrl = URLDecoder.decode(
                            fromJson<FlixHQInitialSourceData>(initialSourceData).link,
                            "UTF-8"
                        )

                        supportedExtractors.mapAsync { extractor ->
                            if (extractor.name == server.name) {
                                extractor.extract(
                                    url = URL(serverUrl),
                                    mediaId = filmId,
                                    episodeId = episodeId,
                                    onLinkLoaded = onLinkLoaded,
                                    onSubtitleLoaded = onSubtitleLoaded
                                )
                            }
                        }
                    }
            }
        }
    }
}