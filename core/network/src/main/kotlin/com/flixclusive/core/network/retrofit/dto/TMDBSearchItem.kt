package com.flixclusive.core.network.retrofit.dto

import com.flixclusive.core.util.film.FilmType
import com.flixclusive.core.util.film.extractYear
import com.flixclusive.model.tmdb.DEFAULT_FILM_SOURCE_NAME
import com.flixclusive.model.tmdb.FilmSearchItem
import com.flixclusive.model.tmdb.Genre
import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Serializable
internal data class TMDBSearchItem(
    @SerializedName("name", alternate = ["title"]) val title: String,
    @SerializedName("backdrop_path") val backdropImage: String? = null,
    @SerializedName("genre_ids") val genreIds: List<Int> = emptyList(),
    @SerializedName("homepage") val homePage: String? = null,
    @SerializedName("original_language") val language: String? = null,
    @SerializedName("poster_path") val posterImage: String? = null,
    @SerializedName("media_type") val mediaType: String? = null,
    @SerializedName("release_date", alternate = ["first_air_date"]) val releaseDate: String? = null,
    @SerializedName("vote_average") val rating: Double? = null,
    @SerializedName("id") val tmdbId: Int,
    val adult: Boolean = false,
    val overview: String? = null,
)

internal fun TMDBSearchItem.toFilmSearchItem(filmType: FilmType): FilmSearchItem {
    val genreName = when (filmType) {
        FilmType.MOVIE -> "Movie"
        FilmType.TV_SHOW -> "TV Show"
    }

    return FilmSearchItem(
        backdropImage = backdropImage,
        providerName = DEFAULT_FILM_SOURCE_NAME,
        genreIds = genreIds,
        language = language,
        posterImage = posterImage,
        rating = rating,
        homePage = homePage,
        title = title,
        tmdbId = tmdbId,
        adult = adult,
        overview = overview,
        releaseDate = releaseDate,
        filmType = filmType,
        id = null,
        year = releaseDate?.extractYear(),
        genres = listOf(
            Genre(
                id = -1,
                name = genreName
            )
        ),
    )
}