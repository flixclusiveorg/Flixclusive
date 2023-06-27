package com.flixclusive.domain.model.tmdb

import kotlinx.serialization.Serializable

@Serializable
data class TMDBEpisode(
    val episodeId: Int = 0,
    val title: String = "",
    val episode: Int = 0,
    val rating: Double = 0.0,
    val season: Int = 0,
    val releaseDate: String = "",
    val description: String = "",
    val image: String? = null,
    val runtime: Int? = null
) : java.io.Serializable