package com.flixclusive.domain.model.tmdb

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Serializable
data class TMDBCollection(
    @SerializedName("backdrop_path") val backdropPath: String?,
    val id: Int,
    @SerializedName("name") val collectionName: String,
    val overview: String?,
    @SerializedName("parts") val films: List<TMDBSearchItem>,
    @SerializedName("poster_path") val posterPath: String?
) : java.io.Serializable