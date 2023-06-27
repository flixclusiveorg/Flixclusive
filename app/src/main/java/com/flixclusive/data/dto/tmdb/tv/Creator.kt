package com.flixclusive.data.dto.tmdb.tv

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Serializable
data class Creator(
    val id: Int,
    @SerializedName("credit_id") val creditId: String,
    val name: String,
    val gender: Int,
    @SerializedName("profile_path") val profilePath: String?
)