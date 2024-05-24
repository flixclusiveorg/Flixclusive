package com.flixclusive.model.configuration

import com.google.gson.annotations.SerializedName

data class GithubLastCommit(
    @SerializedName("object") val lastCommit: GithubCommit
)

data class GithubCommit(
    val sha: String,
    val type: String,
    val url: String
)