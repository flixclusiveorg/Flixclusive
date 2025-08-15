package com.flixclusive.domain.catalog.usecase

import com.flixclusive.core.network.util.Resource
import com.flixclusive.model.film.FilmSearchItem
import com.flixclusive.model.film.SearchResponseData
import com.flixclusive.model.provider.Catalog

/**
 * Use case for fetching a paginated list of items from a specified catalog.
 *
 * This interface defines a method to retrieve items from a catalog,
 * allowing for pagination through the `page` parameter.
 * */
interface PaginateItemsUseCase {
    /**
     * Fetches a paginated list of items from the specified catalog.
     *
     * @param catalog The catalog from which to fetch items.
     * @param page The page number to fetch.
     *
     * @return A [Resource] containing a [SearchResponseData] of [FilmSearchItem]s.
     * */
    suspend operator fun invoke(
        catalog: Catalog,
        page: Int,
    ): Resource<SearchResponseData<FilmSearchItem>>
}
