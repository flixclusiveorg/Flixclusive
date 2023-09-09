package com.flixclusive.domain.model.config

import com.google.gson.annotations.SerializedName

data class AppConfig(
    @SerializedName("maintenance") val isMaintenance: Boolean,
    @SerializedName("build") val build: Long,
    @SerializedName("update_url") val updateUrl: String,
    @SerializedName("tmdb_api_key") val tmdbApiKey: String,
)