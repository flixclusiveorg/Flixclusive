package com.flixclusive.model.tmdb

import kotlinx.serialization.Serializable

@Serializable
data class Genre(
    val id: Int,
    val name: String,
    val mediaType: String? = null,
    val posterPath: String? = null
) : java.io.Serializable
