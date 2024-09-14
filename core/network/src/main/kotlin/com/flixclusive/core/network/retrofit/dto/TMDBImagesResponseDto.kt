package com.flixclusive.core.network.retrofit.dto

import com.flixclusive.model.film.common.details.FilmImage
import kotlinx.serialization.Serializable

@Serializable
data class TMDBImagesResponseDto(
    val backdrops: List<FilmImage>? = null,
    val logos: List<FilmImage>? = null,
    val posters: List<FilmImage>? = null
)