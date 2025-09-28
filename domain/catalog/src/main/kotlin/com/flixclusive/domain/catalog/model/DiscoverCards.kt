package com.flixclusive.domain.catalog.model

import com.flixclusive.data.tmdb.model.TMDBDiscoverCatalog

data class DiscoverCards(
    val categories: List<TMDBDiscoverCatalog>,
    val tvNetworks: List<TMDBDiscoverCatalog>,
    val movieCompanies: List<TMDBDiscoverCatalog>,
) {
    internal val all: List<TMDBDiscoverCatalog> get() = (categories + tvNetworks + movieCompanies).sortedBy { it.name }
}
