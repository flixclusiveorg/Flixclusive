package com.flixclusive.core.network.retrofit.dto

import com.google.gson.annotations.SerializedName

data class GithubReleaseInfo(
    @SerializedName("body") val releaseNotes: String,
    @SerializedName("prerelease") val isPrerelease: Boolean,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("tag_name") val tagName: String,
    val name: String,
)
