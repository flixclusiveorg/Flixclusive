package com.flixclusive.model.tmdb

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Serializable
data class TMDBPageResponse<T>(
    val page: Int = 0,
    val results: List<T> = emptyList(),
    @SerializedName("total_pages") val totalPages: Int = 0,
    @SerializedName("total_results") val totalResults: Int = 0
)
