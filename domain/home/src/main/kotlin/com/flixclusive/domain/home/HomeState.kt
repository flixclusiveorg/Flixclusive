package com.flixclusive.domain.home

import com.flixclusive.core.util.common.resource.Resource
import com.flixclusive.model.provider.ProviderCatalog
import com.flixclusive.model.tmdb.Film
import com.flixclusive.model.tmdb.category.Category

data class HomeState(
    val headerItem: Film? = null,
    val categories: List<Category> = emptyList(),
    val rowItems: List<List<Film>> = emptyList(),
    val rowItemsPagingState: List<PaginationStateInfo> = emptyList(),
    val status: Resource<Unit> = Resource.Loading,
    val providerCatalogs: List<ProviderCatalog> = emptyList()
)