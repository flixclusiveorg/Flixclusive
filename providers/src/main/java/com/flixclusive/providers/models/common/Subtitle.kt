package com.flixclusive.providers.models.common

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class Subtitle(
    val url: String,
    @SerializedName("language") val lang: String
) : Serializable
