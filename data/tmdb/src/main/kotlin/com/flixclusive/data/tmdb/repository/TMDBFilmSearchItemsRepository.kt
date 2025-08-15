package com.flixclusive.data.tmdb.repository

import com.flixclusive.core.network.util.Resource
import com.flixclusive.data.tmdb.util.TMDBFilters.Companion.FILTER_ALL
import com.flixclusive.model.film.FilmSearchItem
import com.flixclusive.model.film.SearchResponseData

/**
 * Repository interface for TMDB film search and pagination.
 */
interface TMDBFilmSearchItemsRepository {
    /**
     * Searches for films and TV shows by query.
     *
     * @param query The search query string
     * @param page The page number for pagination
     * @param filter The media type filter
     * @return [Resource] containing paginated search results
     */
    suspend fun search(
        query: String,
        page: Int,
        filter: Int = FILTER_ALL,
    ): Resource<SearchResponseData<FilmSearchItem>>

    /**
     * Retrieves a paginated list of film search items from a specific URL.
     *
     * @param url The URL to fetch the search items from
     * @param page The page number for pagination
     * @return [Resource] containing paginated search results
     */
    suspend fun get(
        url: String,
        page: Int,
    ): Resource<SearchResponseData<FilmSearchItem>>
}
