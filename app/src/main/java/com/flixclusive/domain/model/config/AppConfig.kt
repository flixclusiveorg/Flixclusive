package com.flixclusive.domain.model.config

import com.google.gson.annotations.SerializedName

data class AppConfig(
    val isMaintenance: Boolean,
    val build: Long,
    @SerializedName("build_codename") val versionName: String,
    @SerializedName("update_url") val updateUrl: String,
    @SerializedName("update_info") val updateInfo: String? = null,
    @SerializedName("tmdb_api_key") val tmdbApiKey: String,
)