package com.flixclusive.model.tmdb

import com.flixclusive.core.util.film.FilmType
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
    val language: String
    val genres: List<Genre>
    val recommendedTitles: List<Recommendation>
}