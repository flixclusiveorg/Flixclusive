package com.flixclusive.core.network.retrofit.dto.common

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable


@Serializable
data class ProductionCountry(
    @SerializedName("iso_3166_1") val iso31661: String,
    val name: String
)
