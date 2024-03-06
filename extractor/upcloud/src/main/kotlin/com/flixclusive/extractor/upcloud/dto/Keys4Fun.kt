package com.flixclusive.extractor.upcloud.dto

import com.google.gson.annotations.SerializedName

data class Keys4Fun(
    @SerializedName("rabbitstream") val e4: RabbitStreamKeys
)

data class RabbitStreamKeys(
    val keys: E4Keys
)

data class E4Keys(
    @SerializedName("v") val kVersion: String,
    @SerializedName("h") val kId: String,
    @SerializedName("b") val browserVersion: String,
    @SerializedName("agent") val userAgent: String,
    val key: String
)