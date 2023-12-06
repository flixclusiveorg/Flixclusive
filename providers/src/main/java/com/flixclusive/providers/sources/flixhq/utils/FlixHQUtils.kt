package com.flixclusive.providers.flixhq.utils

import com.flixclusive.providers.models.common.MediaType
import com.flixclusive.providers.models.common.SearchResultItem
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import java.util.Locale

internal class NoExtractorAvailableException(embed: String) : Exception("No extractor for this server [$embed] yet.")

object FlixHQUtils {

    internal fun Elements.getEpisodeId(episode: Int): String? {
        return try {
            var episodeId: String? = null

            val episodeDoc = select("a.eps-item:contains(Eps $episode)")

            if(episodeDoc.isNotEmpty()) {
                episodeId = episodeDoc.attr("data-id")
            }

            return episodeId
        } catch (e: Exception) {
            null
        }
    }

    internal fun Elements.getSeasonId(season: Int): String? {
        return try {
            var seasonId: String? = null

            if(size == 1 && season == 1 || this[season - 1].text().contains("Season $season")) {
                return this[season - 1].attr("data-id")
            }

            val seasonDoc = select("a:contains(Season ${season})")

            if(seasonDoc.isNotEmpty()) {
                seasonId = seasonDoc.attr("data-id")
            }

            return seasonId
        } catch (e: Exception) {
            null
        }
    }

    internal fun Element.toSearchResultItem(baseUrl: String): SearchResultItem {
        val releaseDate = select("div.film-detail > div.fd-infor > span:nth-child(1)").text()

        return SearchResultItem(
            id = select("div.film-poster > a").attr("href").substring(1),
            title = select("div.film-detail > h2 > a").attr("title"),
            url = "${baseUrl}${select("div.film-poster > a").attr("href")}",
            image = select("div.film-poster > img").attr("data-src"),
            releaseDate = if (releaseDate.isNotBlank() && releaseDate.toIntOrNull() != null) releaseDate else null,
            seasons = if (releaseDate.contains("SS")) releaseDate.split("SS")[1].trim().toIntOrNull() else null,
            mediaType = if (select("div.film-detail > div.fd-infor > span.float-right").text() == "Movie") MediaType.Movie else MediaType.TvShow
        )
    }

    internal fun Elements.getServerName(mediaId: String): String {
        val anchorElement = select("a")
        val titleElement = anchorElement.attr("title")

        return if (mediaId.contains("movie")) {
            titleElement
                .lowercase(Locale.getDefault())
        } else {
            titleElement.substring(6)
                .trim()
                .lowercase(Locale.getDefault())
        }
    }

    internal fun Elements.getServerUrl(
        baseUrl: String,
        mediaId: String
    ): String {
        return "${baseUrl}/${mediaId}.${
            if (!mediaId.contains("movie")) {
                attr("data-id")
            } else {
                attr("data-linkid")
            }
        }".replace(
            if (!mediaId.contains("movie")) {
                Regex("/tv/")
            } else {
                Regex("/movie/")
            },
            if (!mediaId.contains("movie")) {
                "/watch-tv/"
            } else {
                "/watch-movie/"
            }
        )
    }
}