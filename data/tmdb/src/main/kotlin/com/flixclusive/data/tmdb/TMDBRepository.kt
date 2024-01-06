package com.flixclusive.data.tmdb

import com.flixclusive.core.network.retrofit.dto.common.TMDBImagesResponseDto
import com.flixclusive.core.util.common.resource.Resource
import com.flixclusive.model.tmdb.Genre
import com.flixclusive.model.tmdb.Movie
import com.flixclusive.model.tmdb.Season
import com.flixclusive.model.tmdb.TMDBCollection
import com.flixclusive.model.tmdb.TMDBEpisode
import com.flixclusive.model.tmdb.TMDBPageResponse
import com.flixclusive.model.tmdb.TMDBSearchItem
import com.flixclusive.model.tmdb.TvShow

enum class SortOptions {
    POPULARITY,
    RATED,
}

interface TMDBRepository {
    val tmdbApiKey: String

    suspend fun getMovie(
        id: Int
    ): Resource<Movie>

    suspend fun getTvShow(
        id: Int
    ): Resource<TvShow>

    suspend fun getSeason(
        id: Int,
        seasonNumber: Int
    ): Resource<Season>

    suspend fun getTrending(
        mediaType: String = "all",
        timeWindow: String = "week",
        page: Int
    ): Resource<TMDBPageResponse<TMDBSearchItem>>

    suspend fun discoverFilms(
        mediaType: String,
        page: Int,
        withNetworks: List<Int>? = null,
        withCompanies: List<Int>? = null,
        withGenres: List<Genre>? = null,
        sortBy: SortOptions = SortOptions.RATED
    ): Resource<TMDBPageResponse<TMDBSearchItem>>

    suspend fun search(
        mediaType: String,
        query: String,
        page: Int,
    ): Resource<TMDBPageResponse<TMDBSearchItem>>

    suspend fun getImages(
        mediaType: String,
        id: Int,
    ): Resource<TMDBImagesResponseDto>

    suspend fun getEpisode(
        id: Int,
        seasonNumber: Int,
        episodeNumber: Int,
    ): Resource<TMDBEpisode?>

    suspend fun getCollection(id: Int): Resource<TMDBCollection>

    /**
    * A GET request function for custom queries from
    * the HomeCategoriesConfig item
    */
    suspend fun paginateConfigItems(url: String, page: Int): Resource<TMDBPageResponse<TMDBSearchItem>>
}