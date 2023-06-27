package com.flixclusive.domain.model.tmdb

import kotlinx.serialization.Serializable

@Serializable
sealed interface Film {
    val id: Int
    val title: String
    val posterImage: String?
    val backdropImage: String?
    val logoImage: String?
    val overview: String?
    val filmType: FilmType
    val dateReleased: String
    val runtime: String
    val rating: Double
    val genres: List<Genre>
    val recommendedTitles: List<Recommendation>
}