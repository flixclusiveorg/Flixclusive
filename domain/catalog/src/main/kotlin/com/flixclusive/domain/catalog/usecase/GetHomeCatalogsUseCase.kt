package com.flixclusive.domain.catalog.usecase

import com.flixclusive.model.provider.Catalog
import kotlinx.coroutines.flow.Flow

/**
 * Use case to retrieve home catalogs.
 *
 * This use case is responsible for fetching the list of catalogs that are displayed on the home screen.
 * */
interface GetHomeCatalogsUseCase {
    /**
     * Retrieves a list of home catalogs.
     *
     * @return A list of [Catalog] objects representing the home catalogs.
     * */
    operator fun invoke(): Flow<List<Catalog>>
}
