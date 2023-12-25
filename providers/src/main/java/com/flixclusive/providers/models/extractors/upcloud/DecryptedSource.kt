package com.flixclusive.providers.models.extractors.upcloud

import com.google.gson.annotations.SerializedName

data class DecryptedSource(
    @SerializedName("file") val url: String,
    val type: String
)