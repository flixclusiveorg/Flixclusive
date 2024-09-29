package com.flixclusive.model.configuration

data class GithubCommit(
    val sha: String,
) {
    val shortSha: String
        get() = sha.substring(0, 7)
}