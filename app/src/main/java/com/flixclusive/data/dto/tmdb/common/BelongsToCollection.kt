package com.flixclusive.data.dto.tmdb.common

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Serializable
data class BelongsToCollection(
    val id: Int,
    val name: String,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("backdrop_path") val backdropPath: String?
)