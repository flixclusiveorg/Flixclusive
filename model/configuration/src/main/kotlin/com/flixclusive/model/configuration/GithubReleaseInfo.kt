package com.flixclusive.model.configuration

import com.google.gson.annotations.SerializedName

data class GithubReleaseInfo(
    @SerializedName("body") val releaseNotes: String
)