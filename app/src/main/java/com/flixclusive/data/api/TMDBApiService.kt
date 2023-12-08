package com.flixclusive.data.api

import com.flixclusive.data.dto.tmdb.TMDBMovieDto
import com.flixclusive.data.dto.tmdb.TMDBTvShowDto
import com.flixclusive.data.dto.tmdb.common.TMDBImagesResponseDto
import com.flixclusive.data.dto.tmdb.tv.TMDBSeasonDto
import com.flixclusive.domain.model.tmdb.TMDBCollection
import com.flixclusive.domain.model.tmdb.TMDBPageResponse
import com.flixclusive.domain.model.tmdb.TMDBSearchItem
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

interface TMDBApiService {
    @GET("movie/{id}")
    suspend fun getMovie(
        @Path("id") id: Int,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "en-US",
        @Query("append_to_response") appendToResponse: String = "images,recommendations",
        @Query("page") page: Int = 1,
        @Query("include_image_language") includeImageLanguage: String = "en"
    ): TMDBMovieDto

    @GET("tv/{id}")
    suspend fun getTvShow(
        @Path("id") id: Int,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "en-US",
        @Query("append_to_response") appendToResponse: String = "images,recommendations",
        @Query("page") page: Int = 1,
        @Query("include_image_language") includeImageLanguage: String = "en"
    ): TMDBTvShowDto

    @GET("tv/{id}/season/{season_number}")
    suspend fun getSeason(
        @Path("id") id: Int,
        @Path("season_number") seasonNumber: Int,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "en-US",
    ): TMDBSeasonDto

    @GET("trending/{media_type}/{time_window}")
    suspend fun getTrending(
        @Path("media_type") mediaType: String, // others: movie, tv
        @Path("time_window") timeWindow: String, // others: day
        @Query("api_key") apiKey: String,
        @Query("page") page: Int,
        @Query("region") region: String = "US",
    ): TMDBPageResponse<TMDBSearchItem>

    @GET("discover/{media_type}")
    suspend fun discoverFilms(
        @Path("media_type") mediaType: String, // movie, tv
        @Query("api_key") apiKey: String,
        @Query("page") page: Int,
        @Query("sort_by") sortBy: String = "vote_average.desc",
        @Query("with_genres") genres: String = "",
        @Query("with_companies") companies: String = "",
        @Query("with_networks") networks: String = "",
        @Query("without_genres") withoutGenres: String = "10763", // news genre
        @Query("release_date.lte") releasedDate: String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()),
        @Query("with_original_language") withOriginalLanguage: String = "en",
    ): TMDBPageResponse<TMDBSearchItem>


    @GET
    suspend fun get(@Url url: String): TMDBPageResponse<TMDBSearchItem>

    @GET("search/{media_type}")
    suspend fun search(
        @Path("media_type") mediaType: String, // movie, tv, multi
        @Query("api_key") apiKey: String,
        @Query("query") query: String,
        @Query("page") page: Int,
        @Query("language") language: String = "en-US",
        @Query("include_adult") includeAdult: Boolean = false,
        @Query("region") region: String = "US"
    ): TMDBPageResponse<TMDBSearchItem>


    @GET("{media_type}/{id}/images")
    suspend fun getImages(
        @Path("media_type") mediaType: String, // movie, tv,
        @Path("id") id: Int,
        @Query("api_key") apiKey: String,
        @Query("include_image_language") includeImageLanguage: String = "en"
    ): TMDBImagesResponseDto

    @GET("collection/{id}")
    suspend fun getCollection(
        @Path("id") id: Int,
        @Query("api_key") apiKey: String,
    ): TMDBCollection
}