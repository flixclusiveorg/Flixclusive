package com.flixclusive.model.tmdb

import com.flixclusive.core.util.film.FilmType
import kotlinx.serialization.Serializable

@Serializable
sealed interface Film {
    val id: Int
    val title: String
    val posterImage: String?
    val backdropImage: String?
        get() = null
    val logoImage: String?
        get() = null
    val overview: String?
        get() = null
    val filmType: FilmType
    val isReleased: Boolean
    val dateReleased: String
    val runtime: Int?
        get() = null
    val rating: Double
    val language: String
    val genres: List<Genre>
    val recommendedTitles: List<Recommendation>
        get() = emptyList()
}