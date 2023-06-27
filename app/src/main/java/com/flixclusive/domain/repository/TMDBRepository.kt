package com.flixclusive.domain.repository

import com.flixclusive.data.dto.tmdb.common.TMDBImagesResponseDto
import com.flixclusive.domain.common.Resource
import com.flixclusive.domain.model.tmdb.Genre
import com.flixclusive.domain.model.tmdb.Movie
import com.flixclusive.domain.model.tmdb.Season
import com.flixclusive.domain.model.tmdb.TMDBEpisode
import com.flixclusive.domain.model.tmdb.TMDBPageResponse
import com.flixclusive.domain.model.tmdb.TMDBSearchItem
import com.flixclusive.domain.model.tmdb.TvShow

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

    suspend fun getGenres(
        mediaType: String
    ): Resource<List<Genre>>

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
    ): TMDBEpisode?
}