package com.flixclusive.provider.lookmovie.dto

import com.flixclusive.core.util.film.FilmType
import com.flixclusive.provider.base.dto.SearchResultItem
import com.flixclusive.provider.base.dto.SearchResults
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
            filmType = if(inProduction != null) FilmType.TV_SHOW else FilmType.MOVIE,
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