package com.flixclusive.data.tmdb.repository

import com.flixclusive.core.network.util.Resource
import com.flixclusive.data.tmdb.model.TMDBHomeCatalog
import com.flixclusive.data.tmdb.model.TMDBHomeCatalogs
import com.flixclusive.model.film.FilmSearchItem
import com.flixclusive.model.film.SearchResponseData

/**
 * Repository interface for TMDB home catalog content items and configuration-based queries.
 */
interface TMDBHomeCatalogRepository {
    /**
     * Retrieves trending films and TV shows for home screen display.
     *
     * @param mediaType The type of media ("all", "movie", "tv")
     * @param timeWindow The time window ("day", "week")
     * @param page The page number for pagination
     * @return [Resource] containing paginated trending items
     */
    suspend fun getTrending(
        mediaType: String = "all",
        timeWindow: String = "week",
        page: Int,
    ): Resource<SearchResponseData<FilmSearchItem>>

    /**
     * Retrieves all home catalogs data from the asset file.
     *
     * @return [TMDBHomeCatalogs] containing all catalog configurations
     */
    suspend fun getAllCatalogs(): TMDBHomeCatalogs

    /**
     * Retrieves catalogs for a specific media type.
     *
     * @param mediaType The media type ("all", "movie", "tv")
     * @return List of [TMDBHomeCatalog] for the specified media type
     */
    suspend fun getCatalogsForMediaType(mediaType: String): List<TMDBHomeCatalog>

    /**
     * Retrieves only required catalogs for a specific media type.
     *
     * @param mediaType The media type ("all", "movie", "tv")
     * @return List of required [TMDBHomeCatalog] for the specified media type
     */
    suspend fun getRequiredCatalogsForMediaType(mediaType: String): List<TMDBHomeCatalog>
}
