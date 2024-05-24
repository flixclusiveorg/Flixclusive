package com.flixclusive.model.configuration

import com.google.gson.annotations.SerializedName

data class GithubReleaseNotes(
    @SerializedName("body") val releaseNotes: String
)