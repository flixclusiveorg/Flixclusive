package com.flixclusive.providers.sources.lookmovie

import com.flixclusive.providers.interfaces.SourceProvider
import com.flixclusive.providers.models.common.MediaInfo
import com.flixclusive.providers.models.common.MediaType
import com.flixclusive.providers.models.common.SearchResults
import com.flixclusive.providers.models.common.SourceLink
import com.flixclusive.providers.models.common.Subtitle
import com.flixclusive.providers.models.providers.lookmovie.LookMovieMediaDetail
import com.flixclusive.providers.models.providers.lookmovie.LookMovieMediaDetail.Companion.toMediaInfo
import com.flixclusive.providers.models.providers.lookmovie.LookMovieSearchResponse
import com.flixclusive.providers.models.providers.lookmovie.LookMovieSearchResponse.Companion.toSearchResponse
import com.flixclusive.providers.utils.JsonUtils.fromJson
import com.flixclusive.providers.utils.asyncCalls
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
        season: Int?,
        episode: Int?,
        onLinkLoaded: (SourceLink) -> Unit,
        onSubtitleLoaded: (Subtitle) -> Unit,
    ) {
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

        val data = fromJson<LookMovieMediaDetail>(response)

        // For rapid parallel iterations
        asyncCalls(
            {
                (data.subtitles
                    ?.map { it.copy(url = baseUrl + it.url) }
                    ?.distinctBy { it.url }
                    ?: emptyList())
                    .map(onSubtitleLoaded)
            },
            {
                data.streams?.forEach { (key, value) ->
                    onLinkLoaded(SourceLink("$key server", value))
                }
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