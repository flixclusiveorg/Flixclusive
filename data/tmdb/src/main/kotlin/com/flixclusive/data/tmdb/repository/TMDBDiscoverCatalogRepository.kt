package com.flixclusive.data.tmdb.repository

import com.flixclusive.data.tmdb.model.TMDBDiscoverCatalog

/**
 * Repository interface for managing TMDB discover catalogs from assets.
 *
 * This repository provides access to various discovery collections including TV networks,
 * movie companies, genres, and content catalogs. All methods are suspend functions that
 * directly return the data since it's loaded from local assets.
 */
interface TMDBDiscoverCatalogRepository {
    /**
     * Retrieves a list of TV networks available for discovery.
     *
     * @return List of [TMDBDiscoverCatalog] items representing TV networks
     */
    suspend fun getTvNetworks(): List<TMDBDiscoverCatalog>

    /**
     * Retrieves a list of movie production companies available for discovery.
     *
     * @return List of [TMDBDiscoverCatalog] items representing movie companies
     */
    suspend fun getMovieCompanies(): List<TMDBDiscoverCatalog>

    /**
     * Retrieves a list of TV show genres available for discovery.
     *
     * @return List of [TMDBDiscoverCatalog] items representing TV genres
     */
    suspend fun getTvGenres(): List<TMDBDiscoverCatalog>

    /**
     * Retrieves a list of movie genres available for discovery.
     *
     * @return List of [TMDBDiscoverCatalog] items representing movie genres
     */
    suspend fun getMovieGenres(): List<TMDBDiscoverCatalog>

    /**
     * Retrieves a combined list of all genres (both TV and movie) available for discovery.
     *
     * @return List of [TMDBDiscoverCatalog] items representing all genres
     */
    suspend fun getGenres(): List<TMDBDiscoverCatalog>

    /**
     * Retrieves a list of TV show catalogs available for discovery.
     *
     * @return List of [TMDBDiscoverCatalog] items representing TV shows
     */
    suspend fun getTv(): List<TMDBDiscoverCatalog>

    /**
     * Retrieves a list of movie catalogs available for discovery.
     *
     * @return List of [TMDBDiscoverCatalog] items representing movies
     */
    suspend fun getMovies(): List<TMDBDiscoverCatalog>
}
