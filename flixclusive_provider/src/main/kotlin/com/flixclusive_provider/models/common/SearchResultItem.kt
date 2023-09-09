package com.flixclusive_provider.models.common

data class SearchResultItem(
    val id: String,
    val title: String?,
    val url: String?,
    val image: String?,
    val releaseDate: String?,
    val mediaType: MediaType?,
    val seasons: Int?
)
