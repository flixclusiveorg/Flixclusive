package com.flixclusive.providers.sources.lookmovie

import com.flixclusive.providers.interfaces.SourceProvider
import com.flixclusive.providers.models.common.MediaInfo
import com.flixclusive.providers.models.common.MediaType
import com.flixclusive.providers.models.common.SearchResults
import com.flixclusive.providers.models.common.VideoData
import com.flixclusive.providers.models.common.VideoDataServer
import com.flixclusive.providers.models.providers.lookmovie.LookMovieMediaDetail
import com.flixclusive.providers.models.providers.lookmovie.LookMovieMediaDetail.Companion.toMediaInfo
import com.flixclusive.providers.models.providers.lookmovie.LookMovieSearchResponse
import com.flixclusive.providers.models.providers.lookmovie.LookMovieSearchResponse.Companion.toSearchResponse
import com.flixclusive.providers.utils.JsonUtils.fromJson
import com.flixclusive.providers.utils.network.OkHttpUtils
import com.flixclusive.providers.utils.network.OkHttpUtils.asString
import com.flixclusive.providers.utils.replaceWhitespaces
import okhttp3.OkHttpClient

class LookMovie(client: OkHttpClient) : SourceProvider(client) {
    override val name: String = "LookMovie"
    override val baseUrl: String = "https://lmscript.xyz"

    override suspend fun search(query: String, page: Int, mediaType: MediaType): SearchResults {
        val mode = if (mediaType == MediaType.Movie) "movies" else "shows"
        val uri = OkHttpUtils.GET(
            "$baseUrl/v1/$mode?filters[q]=${
                query.replaceWhitespaces("+")
            }&page=$page"
        )

        val data = client.newCall(uri).execute()
            .body
            ?.charStream()
            ?.asString()
            ?: throw Exception("Error searching on LookMovie")

        return fromJson<LookMovieSearchResponse>(data).toSearchResponse()
    }

    override suspend fun getMediaInfo(mediaId: String, mediaType: MediaType): MediaInfo =
        getTvShow(mediaId).toMediaInfo()

    override suspend fun getSourceLinks(
        mediaId: String,
        server: String?,
        season: Int?,
        episode: Int?,
    ): VideoData {
        val isTvShow = season != null || episode != null
        val mode = if (isTvShow) "episodes" else "movies"

        var mediaIdToUse = mediaId

        if (isTvShow) {
            val data = getTvShow(mediaIdToUse)

            val newId = data.episodes
                ?.find {
                    it.episode == episode && it.season == season
                }?.id ?: throw NullPointerException("Couldn't find episode/season queried.")

            mediaIdToUse = newId.toString()
        }

        val response = client.newCall(
            OkHttpUtils.GET(
                "$baseUrl/v1/$mode/view?expand=streams,subtitles&id=$mediaIdToUse"
            )
        ).execute()
            .body
            ?.charStream()
            ?.asString()
            ?: throw Exception("Error getting $mediaIdToUse data from LookMovie")

        val possibleServers = arrayOf(
            "auto",
            "1080p",
            "1080",
            "720p",
            "720",
            "480p",
            "480",
            "240p",
            "240",
            "360p",
            "360",
            "144",
            "144p"
        )

        val data = fromJson<LookMovieMediaDetail>(response)
        var sourceToUse = if (server != null && possibleServers.contains(server)) {
            data.streams?.get(server)
        } else null

        if (sourceToUse == null) {
            sourceToUse = data.streams?.values?.first()
                ?: throw NullPointerException("Cannot get path source.")
        }

        return VideoData(
            mediaId = mediaId,
            source = sourceToUse,
            subtitles = data.subtitles
                ?.map { it.copy(url = baseUrl + it.url) }
                ?.distinctBy { it.url }
                ?: emptyList(),
            sourceName = name,
            servers = data.streams?.map { (key, value) ->
                VideoDataServer("$key Server", value)
            }
        )
    }

    private fun getTvShow(mediaId: String): LookMovieMediaDetail {
        val data = client.newCall(
            OkHttpUtils.GET(
                "$baseUrl/v1/shows?expand=episodes&id=$mediaId"
            )
        ).execute()
            .body
            ?.charStream()
            ?.asString()
            ?: throw Exception("Error getting $mediaId from LookMovie")

        return fromJson<LookMovieMediaDetail>(data)
    }
}