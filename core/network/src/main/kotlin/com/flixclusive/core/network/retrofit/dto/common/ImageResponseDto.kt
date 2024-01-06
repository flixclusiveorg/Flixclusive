package com.flixclusive.core.network.retrofit.dto.common

import kotlinx.serialization.Serializable

@Serializable
data class TMDBImagesResponseDto(
    val backdrops: List<TMDBImageDto> = emptyList(),
    val id: Int = 0,
    val logos: List<TMDBImageDto> = emptyList(),
    val posters: List<TMDBImageDto> = emptyList()
)