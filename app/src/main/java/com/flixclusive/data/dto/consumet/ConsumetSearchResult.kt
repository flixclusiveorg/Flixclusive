package com.flixclusive.data.dto.consumet

data class ConsumetSearchResult(
    val currentPage: String = "1",
    val hasNextPage: Boolean = false,
    val results: List<ConsumetSearchItem> = emptyList()
)

data class ConsumetSearchItem(
    val id: String = "",
    val title: String = "",
    val url: String = "",
    val image: String = "",
    val releaseDate: String? = null,
    val seasons: Int? = null,
    val type: String = ""
)
