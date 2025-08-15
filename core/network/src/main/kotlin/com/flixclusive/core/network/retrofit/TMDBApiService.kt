package com.flixclusive.core.network.retrofit

import com.flixclusive.core.network.retrofit.dto.TMDBImagesResponseDto
import com.flixclusive.model.film.FilmSearchItem
import com.flixclusive.model.film.Movie
import com.flixclusive.model.film.SearchResponseData
import com.flixclusive.model.film.TMDBCollection
import com.flixclusive.model.film.TvShow
import com.flixclusive.model.film.common.tv.Season
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url

const val TMDB_API_BASE_URL = "https://api.themoviedb.org/3/"
internal const val TMDB_APPEND_TO_RESPONSE = "images,recommendations,external_ids,credits"

interface TMDBApiService {
    @GET("movie/{id}")
    suspend fun getMovie(
        @Path("id") id: Int,
        @Query("language") language: String = "en-US",
        @Query("append_to_response") appendToResponse: String = TMDB_APPEND_TO_RESPONSE,
        @Query("page") page: Int = 1,
        @Query("include_image_language") includeImageLanguage: String = "en",
    ): Movie

    @GET("tv/{id}")
    suspend fun getTvShow(
        @Path("id") id: Int,
        @Query("language") language: String = "en-US",
        @Query("append_to_response") appendToResponse: String = TMDB_APPEND_TO_RESPONSE,
        @Query("page") page: Int = 1,
        @Query("include_image_language") includeImageLanguage: String = "en",
    ): TvShow

    @GET("tv/{id}/season/{season_number}")
    suspend fun getSeason(
        @Path("id") id: Int,
        @Path("season_number") seasonNumber: Int,
        @Query("language") language: String = "en-US",
    ): Season

    @GET("trending/{media_type}/{time_window}")
    suspend fun getTrending(
        @Path("media_type") mediaType: String, // others: movie, tv
        @Path("time_window") timeWindow: String, // others: day
        @Query("page") page: Int,
        @Query("region") region: String = "US",
    ): SearchResponseData<FilmSearchItem>

    @GET("search/{media_type}")
    suspend fun search(
        @Path("media_type") mediaType: String, // movie, tv, multi
        @Query("query") query: String,
        @Query("page") page: Int,
        @Query("language") language: String = "en-US",
        @Query("include_adult") includeAdult: Boolean = false,
        @Query("region") region: String = "US",
    ): SearchResponseData<FilmSearchItem>

    @GET("{media_type}/{id}/images")
    suspend fun getImages(
        @Path("media_type") mediaType: String, // movie, tv,
        @Path("id") id: Int,
        @Query("include_image_language") includeImageLanguage: String? = "en",
    ): TMDBImagesResponseDto

    @GET("collection/{id}")
    suspend fun getCollection(
        @Path("id") id: Int,
    ): TMDBCollection

    @GET
    suspend fun get(
        @Url url: String,
    ): SearchResponseData<FilmSearchItem>
}
