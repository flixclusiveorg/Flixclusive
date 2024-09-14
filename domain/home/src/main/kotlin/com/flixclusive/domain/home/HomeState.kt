package com.flixclusive.domain.home

import com.flixclusive.core.network.util.Resource
import com.flixclusive.model.film.Film
import com.flixclusive.model.provider.Catalog
import com.flixclusive.model.provider.ProviderCatalog

data class HomeState(
    val headerItem: Film? = null,
    val catalogs: List<Catalog> = emptyList(),
    val rowItems: List<List<Film>> = emptyList(),
    val rowItemsPagingState: List<PaginationStateInfo> = emptyList(),
    val status: Resource<Unit> = Resource.Loading,
    val providerCatalogs: List<ProviderCatalog> = emptyList()
)