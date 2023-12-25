package com.flixclusive.domain.model.tmdb

import java.io.Serializable

@kotlinx.serialization.Serializable
data class FilmImpl(
    override val id: Int = 0,
    override val title: String = "",
    override val posterImage: String? = null,
    override val backdropImage: String? = null,
    override val logoImage: String? = null,
    override val overview: String? = null,
    override val filmType: FilmType = FilmType.MOVIE,
    override val dateReleased: String = "",
    override val runtime: String = "",
    override val rating: Double = 0.0,
    override val language: String = "en",
    override val genres: List<Genre> = emptyList(),
    override val recommendedTitles: List<Recommendation> = emptyList()
) : Film, Serializable

fun Film.toFilmInstance(): FilmImpl {
    return FilmImpl(
        id = id,
        title = title,
        posterImage = posterImage,
        backdropImage = backdropImage,
        logoImage = logoImage,
        overview = overview,
        filmType = filmType,
        dateReleased = dateReleased,
        runtime = runtime,
        rating = rating,
        genres = genres,
        language = language,
        recommendedTitles = recommendedTitles
    )
}