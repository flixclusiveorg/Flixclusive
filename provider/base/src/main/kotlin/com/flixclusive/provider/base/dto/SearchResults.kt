package com.flixclusive.provider.base.dto

data class SearchResults(
    val currentPage: Int = 1,
    val hasNextPage: Boolean = false,
    val results: List<SearchResultItem> = emptyList()
)