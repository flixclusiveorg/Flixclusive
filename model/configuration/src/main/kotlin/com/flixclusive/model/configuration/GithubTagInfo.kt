package com.flixclusive.model.configuration

import com.google.gson.annotations.SerializedName

data class GithubTagInfo(
    val name: String,
    @SerializedName("commit") val lastCommit: GithubCommit,
)