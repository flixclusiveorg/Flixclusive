package com.flixclusive.model.tmdb

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Serializable
data class TMDBCollection(
    val id: Int,
    val overview: String? = null,
    @SerializedName("parts") val films: List<FilmSearchItem>,
    @SerializedName("backdrop_path") val backdropPath: String?,
    @SerializedName("name") val collectionName: String,
    @SerializedName("poster_path") val posterPath: String?
) : java.io.Serializable