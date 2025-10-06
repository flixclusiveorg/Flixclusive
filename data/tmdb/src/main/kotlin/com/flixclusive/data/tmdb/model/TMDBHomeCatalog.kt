package com.flixclusive.data.tmdb.model

import com.flixclusive.model.provider.Catalog

/**
 * A catalog model used for the home screen and dashboard content.
 *
 * This model contains queries that are fetched through GET requests
 * via the TMDBHomeCatalogRepository. The hardcoded query URLs enable
 * efficient pagination and consistent content delivery for home screen
 * catalog sections.
 */
data class TMDBHomeCatalog(
    val required: Boolean,
    override val url: String,
    override val name: String,
    override val image: String? = null,
) : Catalog() {
    override val canPaginate: Boolean get() = true
}

/**
 * A collection of [TMDBHomeCatalog]s organized by media type for TMDB home operations.
 *
 * This model wraps the response from the home_catalogs.json asset file,
 * providing organized access to home screen catalog configurations.
 */
data class TMDBHomeCatalogs(
    val all: List<TMDBHomeCatalog>,
    val movie: List<TMDBHomeCatalog>,
    val tv: List<TMDBHomeCatalog>,
)
