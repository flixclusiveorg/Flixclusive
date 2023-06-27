package com.flixclusive.data.dto.tmdb.common

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Serializable
data class TMDBImageDto(
    @SerializedName("aspect_ratio") val aspectRatio: Double,
    val height: Int,
    @SerializedName("iso_639_1") val iso6391: String? = null,
    @SerializedName("file_path") val filePath: String,
    @SerializedName("vote_average") val voteAverage: Double,
    @SerializedName("vote_count") val voteCount: Int,
    val width: Int
)


