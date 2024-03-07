package com.flixclusive.provider.dto

data class SearchResults(
    val currentPage: Int = 1,
    val hasNextPage: Boolean = false,
    val results: List<SearchResultItem> = emptyList()
)