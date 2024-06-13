package com.flixclusive.model.configuration

import com.google.gson.annotations.SerializedName

data class GithubBranchInfo(
    @SerializedName("object") val lastCommit: GithubCommit
)