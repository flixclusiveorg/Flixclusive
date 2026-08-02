package com.flixclusive.core.database.entity.downloads

data class DownloadStreamCandidate(
    val url: String,
    val headers: Map<String, String>? = null,
    val isHls: Boolean = false,
)
