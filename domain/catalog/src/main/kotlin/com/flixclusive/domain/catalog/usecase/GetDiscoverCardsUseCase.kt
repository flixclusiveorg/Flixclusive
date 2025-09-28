package com.flixclusive.domain.catalog.usecase

import com.flixclusive.core.network.util.Resource
import com.flixclusive.data.tmdb.model.TMDBDiscoverCatalog
import com.flixclusive.domain.catalog.model.DiscoverCards

/**
 * Use case for retrieving discover cards from TMDB.
 * */
interface GetDiscoverCardsUseCase {
    /**
     * Invokes the use case to get a list of TMDB discover catalogs.
     *
     * @return A [Resource] containing a list of [TMDBDiscoverCatalog] items.
     * */
    suspend operator fun invoke(): Resource<DiscoverCards>
}
