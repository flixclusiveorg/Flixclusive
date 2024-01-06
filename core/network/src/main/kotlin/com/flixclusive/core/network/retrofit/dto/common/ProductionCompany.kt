package com.flixclusive.core.network.retrofit.dto.common

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable


@Serializable
data class ProductionCompany(
    val id: Int,
    @SerializedName("logo_path") val logoPath: String?,
    val name: String,
    @SerializedName("origin_country") val originCountry: String,
)
