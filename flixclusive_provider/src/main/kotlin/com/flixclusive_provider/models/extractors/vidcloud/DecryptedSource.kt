package com.flixclusive_provider.models.extractors.vidcloud

import com.google.gson.annotations.SerializedName

data class DecryptedSource(
    @SerializedName("file") val url: String,
    val type: String
)