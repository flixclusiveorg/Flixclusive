package com.flixclusive.model.film.common.details

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Serializable
data class FilmImage(
    @SerializedName("file_path") val filePath: String,
    val height: Int,
    val width: Int
)


