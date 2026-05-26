package com.flixclusive.domain.catalog.usecase

import com.flixclusive.core.common.domain.Async
import com.flixclusive.model.media.PartialMedia
import com.flixclusive.model.media.common.PaginatedMedia
import com.flixclusive.model.provider.Catalog
import kotlinx.coroutines.flow.Flow

/**
 * Use case for fetching a paginated list of items from a specified catalog.
 *
 * This interface defines a method to retrieve items from a catalog,
 * allowing for pagination through the `page` parameter.
 * */
interface GetCatalogItemsUseCase {
    /**
     * Fetches a paginated list of items from the specified catalog.
     *
     * @param catalog The catalog from which to fetch items.
     * @param page The page number to fetch.
     *
     * @return A [Async] containing a [PaginatedMedia] of [PartialMedia]s.
     * */
    operator fun invoke(
        catalog: Catalog,
        page: Int,
    ): Flow<Async<PaginatedMedia<PartialMedia>>>
}
