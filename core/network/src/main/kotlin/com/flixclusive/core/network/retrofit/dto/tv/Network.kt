package com.flixclusive.core.network.retrofit.dto.tv

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable


@Serializable
data class Network(
    val name: String,
    val id: Int,
    @SerializedName("logo_path") val logoPath: String?,
    @SerializedName("origin_country") val originCountry: String,
)