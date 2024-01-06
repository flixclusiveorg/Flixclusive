package com.flixclusive.model.tmdb

import kotlinx.serialization.Serializable

@Serializable
data class Season(
    val seasonNumber: Int = 0,
    val image: String? = null,
    val name: String = "",
    val episodes: List<TMDBEpisode> = emptyList(),
    val isReleased: Boolean = false
) : java.io.Serializable

