package com.flixclusive.providers.models.common

data class SearchResultItem(
    val id: String? = null,
    val title: String? = null,
    val url: String? = null,
    val image: String? = null,
    val releaseDate: String? = null,
    val mediaType: MediaType? = null,
    val seasons: Int? = null
)
