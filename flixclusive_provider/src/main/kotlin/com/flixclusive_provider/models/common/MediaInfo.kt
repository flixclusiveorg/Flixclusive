package com.flixclusive_provider.models.common

data class MediaInfo(
    val id: String,
    val title: String,
    val url: String,
    val releaseDate: String = "",
    val seasons: List<Season>? = null,
    val episode: Episode? = null,
)

data class Season(
    val season: Int,
    val episodes: List<Episode>
)

data class Episode(
    val id: String,
    val title: String,
    val url: String,
    val episode: Int? = null,
    val season: Int? = null,
)
