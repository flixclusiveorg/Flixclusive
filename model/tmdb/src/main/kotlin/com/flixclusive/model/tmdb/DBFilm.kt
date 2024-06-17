package com.flixclusive.model.tmdb

import com.flixclusive.core.util.film.FilmType
import kotlinx.serialization.SerialName
import java.io.Serializable

data class DBFilm(
    @SerialName("_id")
    override val id: String? = null,
    override val providerName: String? = null,
    override val imdbId: String? = null,
    @SerialName("id")
    override val tmdbId: Int? = null,
    override val language: String? = null,
    override val adult: Boolean = false,
    override val title: String = "",
    override val runtime: Int? = null,
    override val backdropImage: String? = null,
    override val posterImage: String? = null,
    override val overview: String? = null,
    override val homePage: String? = null,
    @SerialName("dateReleased") override val releaseDate: String? = null,
    override val logoImage: String? = null,
    override val year: Int? = null,
    @SerialName("recommendedTitles") override val recommendations: List<FilmSearchItem> = emptyList(),
    override val filmType: FilmType = FilmType.MOVIE,
    override val rating: Double? = null
) : Film(), Serializable

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
        providerName = providerName
    )
}