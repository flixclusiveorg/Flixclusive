package com.flixclusive.data.tmdb

import com.flixclusive.core.network.util.Resource
import com.flixclusive.model.film.FilmSearchItem
import com.flixclusive.model.film.Genre
import com.flixclusive.model.film.Movie
import com.flixclusive.model.film.SearchResponseData
import com.flixclusive.model.film.TMDBCollection
import com.flixclusive.model.film.TvShow
import com.flixclusive.model.film.common.tv.Episode
import com.flixclusive.model.film.common.tv.Season
import com.flixclusive.model.provider.link.Stream

enum class SortOptions {
    POPULARITY,
    RATED,
}

interface TMDBRepository {
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
    ): Resource<SearchResponseData<FilmSearchItem>>

    suspend fun discoverFilms(
        mediaType: String,
        page: Int,
        withNetworks: List<Int>? = null,
        withCompanies: List<Int>? = null,
        withGenres: List<Genre>? = null,
        sortBy: SortOptions = SortOptions.RATED
    ): Resource<SearchResponseData<FilmSearchItem>>

    suspend fun search(
        query: String,
        page: Int,
        filter: Int = FILTER_ALL,
    ): Resource<SearchResponseData<FilmSearchItem>>

    suspend fun getLogo(
        mediaType: String,
        id: Int,
    ): Resource<String>

    suspend fun getEpisode(
        id: Int,
        seasonNumber: Int,
        episodeNumber: Int,
    ): Resource<Episode?>

    suspend fun getCollection(id: Int): Resource<TMDBCollection>

    /**
    * A GET request function for custom queries from
    * the HomeCatalogsConfig item
    */
    suspend fun paginateConfigItems(url: String, page: Int): Resource<SearchResponseData<FilmSearchItem>>

    suspend fun getWatchProviders(mediaType: String, id: Int): Resource<List<Stream>>
}