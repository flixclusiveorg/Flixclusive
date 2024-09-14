package com.flixclusive.model.film.common.details

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable


@Serializable
data class Company(
    val id: Int,
    @SerializedName("logo_path") val logoPath: String?,
    val name: String,
) : java.io.Serializable
