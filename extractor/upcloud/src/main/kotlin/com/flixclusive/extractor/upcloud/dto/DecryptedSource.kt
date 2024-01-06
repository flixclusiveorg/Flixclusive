package com.flixclusive.extractor.upcloud.dto

import com.google.gson.annotations.SerializedName

data class DecryptedSource(
    @SerializedName("file") val url: String,
    val type: String
)