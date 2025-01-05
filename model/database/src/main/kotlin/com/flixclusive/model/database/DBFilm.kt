package com.flixclusive.model.database

import com.flixclusive.model.film.Film
import com.flixclusive.model.film.FilmSearchItem
import com.flixclusive.model.film.util.FilmType
import java.io.Serializable

/**
 *
 * Used by database module
 * */
data class DBFilm(
    override val id: String? = null,
    override val providerName: String? = null,
    override val imdbId: String? = null,
    override val tmdbId: Int? = null,
    override val language: String? = null,
    override val adult: Boolean = false,
    override val title: String = "",
    override val runtime: Int? = null,
    override val backdropImage: String? = null,
    override val posterImage: String? = null,
    override val overview: String? = null,
    override val homePage: String? = null,
    override val releaseDate: String? = null,
    override val logoImage: String? = null,
    override val year: Int? = null,
    override val recommendations: List<FilmSearchItem> = emptyList(),
    override val filmType: FilmType = FilmType.MOVIE,
    override val rating: Double? = null,
    override val customProperties: Map<String, String?> = emptyMap(),
) : Film(), Serializable


/**
 * Converts a [Film] to a [DBFilm].
 * */
fun Film.toFilmInstance(): DBFilm {
    return DBFilm(
        id = id,
        imdbId = imdbId,
        tmdbId = tmdbId,
        adult = adult,
        title = title,
        posterImage = posterImage,
        backdropImage = backdropImage,
        logoImage = logoImage,
        overview = overview,
        filmType = filmType,
        rating = rating,
        language = language,
        recommendations = recommendations,
        releaseDate = releaseDate,
        year = year,
        runtime = runtime,
        homePage = homePage,
        providerName = providerName,
        customProperties = customProperties
    )
}