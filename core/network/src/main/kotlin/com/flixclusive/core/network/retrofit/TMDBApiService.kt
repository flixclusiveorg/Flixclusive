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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

const val TMDB_API_BASE_URL = "https://api.themoviedb.org/3/"
internal const val TMDB_APPEND_TO_RESPONSE = "images,recommendations,external_ids,credits"

interface TMDBApiService {
    @GET("movie/{id}")
    suspend fun getMovie(
        @Path("id") id: Int,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "en-US",
        @Query("append_to_response") appendToResponse: String = TMDB_APPEND_TO_RESPONSE,
        @Query("page") page: Int = 1,
        @Query("include_image_language") includeImageLanguage: String = "en"
    ): Movie

    @GET("tv/{id}")
    suspend fun getTvShow(
        @Path("id") id: Int,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "en-US",
        @Query("append_to_response") appendToResponse: String = TMDB_APPEND_TO_RESPONSE,
        @Query("page") page: Int = 1,
        @Query("include_image_language") includeImageLanguage: String = "en"
    ): TvShow

    @GET("tv/{id}/season/{season_number}")
    suspend fun getSeason(
        @Path("id") id: Int,
        @Path("season_number") seasonNumber: Int,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "en-US",
    ): Season

    @GET("trending/{media_type}/{time_window}")
    suspend fun getTrending(
        @Path("media_type") mediaType: String, // others: movie, tv
        @Path("time_window") timeWindow: String, // others: day
        @Query("api_key") apiKey: String,
        @Query("page") page: Int,
        @Query("region") region: String = "US",
    ): SearchResponseData<FilmSearchItem>

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
    ): SearchResponseData<FilmSearchItem>


    @GET
    suspend fun get(@Url url: String): SearchResponseData<FilmSearchItem>

    @GET("search/{media_type}")
    suspend fun search(
        @Path("media_type") mediaType: String, // movie, tv, multi
        @Query("api_key") apiKey: String,
        @Query("query") query: String,
        @Query("page") page: Int,
        @Query("language") language: String = "en-US",
        @Query("include_adult") includeAdult: Boolean = false,
        @Query("region") region: String = "US"
    ): SearchResponseData<FilmSearchItem>


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