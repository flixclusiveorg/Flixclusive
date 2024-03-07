package com.flixclusive.provider.dto

import com.flixclusive.core.util.film.FilmType


data class SearchResultItem(
    val id: String? = null,
    val tmdbId: Int? = null,
    val title: String? = null,
    val url: String? = null,
    val image: String? = null,
    val releaseDate: String? = null,
    val filmType: FilmType? = null,
    val seasons: Int? = null
)
