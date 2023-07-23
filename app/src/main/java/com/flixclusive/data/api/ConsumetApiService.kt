package com.flixclusive.data.api

import com.flixclusive.data.dto.consumet.ConsumetFilmDto
import com.flixclusive.data.dto.consumet.ConsumetSearchResult
import com.flixclusive.domain.model.consumet.VideoData
import com.flixclusive.domain.model.consumet.VideoDataServer
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ConsumetApiService {
    @GET("/movies/{provider}/watch")
    suspend fun getStreamingLinks(
        @Path("provider") provider: String,
        @Query("episodeId") episodeId: String,
        @Query("mediaId") mediaId: String,
        @Query("server") server: String,
    ): VideoData

    @GET("/movies/{provider}/servers")
    suspend fun getAvailableServers(
        @Path("provider") provider: String,
        @Query("episodeId") episodeId: String,
        @Query("mediaId") mediaId: String,
    ): List<VideoDataServer>

    @GET("movies/{provider}/{query}")
    suspend fun searchStreamingLinks(
        @Path("provider") provider: String,
        @Path("query") query: String,
        @Query("page") page: Int = 1,
    ): ConsumetSearchResult

    @GET("movies/{provider}/info")
    suspend fun getTvShow(
        @Path("provider") provider: String,
        @Query("id") id: String,
    ): ConsumetFilmDto
}