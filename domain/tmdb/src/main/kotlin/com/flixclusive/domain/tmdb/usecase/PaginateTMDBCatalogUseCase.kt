package com.flixclusive.domain.tmdb.usecase

import com.flixclusive.core.network.util.Resource
import com.flixclusive.model.film.FilmSearchItem
import com.flixclusive.model.film.SearchResponseData

/**
 * Use case for paginating TMDB catalog queries.
 *
 * This use case handles the execution of paginated requests to TMDB API endpoints,
 * typically used for home catalogs and discovery content.
 */
interface PaginateTMDBCatalogUseCase {
    /**
     * Executes a paginated TMDB catalog query.
     *
     * @param url The relative URL for the TMDB API endpoint (without base URL)
     * @param page The page number for pagination (1-based)
     * @return [Resource] containing paginated search results or error information
     */
    suspend operator fun invoke(
        url: String,
        page: Int,
    ): Resource<SearchResponseData<FilmSearchItem>>
}
