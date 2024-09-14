package com.flixclusive.model.film

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

/**
 * Represents the data returned from a search request.
 *
 * @property page The current page number of the search results.
 * @property results The list of search results for the current page.
 * @property hasNextPage Indicates whether there are more pages of search results available.
 * @property totalPages The total number of pages of search results.
 *
 * @see FilmSearchItem
 */
@Serializable
data class SearchResponseData<T>(
    val page: Int = 1,
    val results: List<T> = emptyList(),
    val hasNextPage: Boolean = false,
    @SerializedName("total_pages") val totalPages: Int = 0
)
