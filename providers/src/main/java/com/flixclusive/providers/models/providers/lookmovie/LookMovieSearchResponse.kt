package com.flixclusive.providers.models.providers.lookmovie

import com.flixclusive.providers.models.common.MediaType
import com.flixclusive.providers.models.common.SearchResultItem
import com.flixclusive.providers.models.common.SearchResults
import com.google.gson.annotations.SerializedName

data class LookMovieSearchResponse(
    val items: List<LookMovieSearchItem>,
    @SerializedName("_meta") val pageResult: Result
) {
    data class Result(
        val totalCount: Int? = 0,
        val pageCount: Int? = 0,
        val currentPage: Int? = 1,
    )
    data class LookMovieSearchItem(
        @SerializedName("id_show", alternate = ["id_movie"]) val id: Int? = null,
        val title: String = "",
        @SerializedName("tmdb_id", alternate = ["tmdb_prefix"]) val tmdbId: Int? = null,
        @SerializedName("in_production") val inProduction: Int? = null,
        val year: Int? = null
    )

    companion object {
        private fun LookMovieSearchItem.toSearchResultItem() = SearchResultItem(
            id = id.toString(),
            tmdbId = tmdbId,
            title = title,
            releaseDate = year.toString(),
            mediaType = if(inProduction != null) MediaType.TvShow else MediaType.Movie,
        )

        fun LookMovieSearchResponse.toSearchResponse(): SearchResults {
            val currentPage = pageResult.currentPage ?: 1
            val pageCount = pageResult.pageCount ?: 0

            return SearchResults(
                currentPage = currentPage,
                hasNextPage = currentPage < pageCount && pageCount > 0,
                results = items.map { it.toSearchResultItem() }
            )
        }
    }
}