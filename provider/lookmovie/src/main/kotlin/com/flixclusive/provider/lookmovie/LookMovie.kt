package com.flixclusive.provider.lookmovie

import com.flixclusive.core.util.coroutines.asyncCalls
import com.flixclusive.core.util.film.FilmType
import com.flixclusive.core.util.json.fromJson
import com.flixclusive.core.util.network.GET
import com.flixclusive.core.util.network.asString
import com.flixclusive.model.provider.SourceLink
import com.flixclusive.model.provider.Subtitle
import com.flixclusive.model.provider.SubtitleSource
import com.flixclusive.provider.base.Provider
import com.flixclusive.provider.base.dto.FilmInfo
import com.flixclusive.provider.base.dto.SearchResults
import com.flixclusive.provider.base.util.replaceWhitespaces
import com.flixclusive.provider.lookmovie.dto.LookMovieMediaDetail
import com.flixclusive.provider.lookmovie.dto.LookMovieMediaDetail.Companion.toMediaInfo
import com.flixclusive.provider.lookmovie.dto.LookMovieSearchResponse
import com.flixclusive.provider.lookmovie.dto.LookMovieSearchResponse.Companion.toSearchResponse
import okhttp3.OkHttpClient

class LookMovie(client: OkHttpClient) : Provider(client) {
    override val name: String = "LookMovie"
    override val baseUrl: String = "https://lmscript.xyz"

    override suspend fun search(
        query: String,
        page: Int,
        filmType: FilmType,
    ): SearchResults {
        val mode = if (filmType == FilmType.MOVIE) "movies" else "shows"
        val uri = GET(
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

    override suspend fun getFilmInfo(filmId: String, filmType: FilmType): FilmInfo = getTvShow(filmId).toMediaInfo()

    override suspend fun getSourceLinks(
        filmId: String,
        season: Int?,
        episode: Int?,
        onLinkLoaded: (SourceLink) -> Unit,
        onSubtitleLoaded: (Subtitle) -> Unit,
    ) {
        val isTvShow = season != null || episode != null
        val mode = if (isTvShow) "episodes" else "movies"

        var mediaIdToUse = filmId

        if (isTvShow) {
            val data = getTvShow(mediaIdToUse)

            val newId = data.episodes
                ?.find {
                    it.episode == episode && it.season == season
                }?.id ?: throw NullPointerException("Couldn't find episode/season queried.")

            mediaIdToUse = newId.toString()
        }

        val response = client.newCall(
            GET(
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
                    ?.map {
                        it.copy(
                            url = baseUrl + it.url,
                            type = SubtitleSource.ONLINE
                        )
                    }
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
            GET(
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