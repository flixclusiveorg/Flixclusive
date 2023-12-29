package com.flixclusive.providers.models.common

data class MediaInfo(
    val id: String,
    val title: String,
    val yearReleased: String = "",
    val seasons: Int? = null,
    val episodes: Int? = null
)
