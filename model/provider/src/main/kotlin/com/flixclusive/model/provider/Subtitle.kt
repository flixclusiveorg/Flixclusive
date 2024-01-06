package com.flixclusive.model.provider

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class Subtitle(
    val url: String,
    @SerializedName("language") val lang: String
) : Serializable
