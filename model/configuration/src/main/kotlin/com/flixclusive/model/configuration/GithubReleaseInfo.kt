package com.flixclusive.model.configuration

import com.google.gson.annotations.SerializedName

data class GithubReleaseInfo(
    @SerializedName("body") val releaseNotes: String,
    @SerializedName("prerelease") val isPrerelease: Boolean,
    @SerializedName("created_at") val createdAt: String,
    val name: String,
)