package com.flixclusive.data.dto.consumet

data class ConsumetFilmDto(
    val id: String = "",
    val title: String = "",
    val url: String = "",
    val cover: String = "",
    val image: String = "",
    val description: String = "",
    val type: String = "",
    val releaseDate: String = "",
    val genres: List<String> = emptyList(),
    val casts: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val production: String = "",
    val country: String = "",
    val duration: String = "",
    val rating: Double = 0.0,
    val recommendations: List<ConsumetRecommendationDto> = emptyList(),
    val episodes: List<ConsumetEpisode> = emptyList()
)