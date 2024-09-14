package com.flixclusive.core.network.retrofit.dto

import com.flixclusive.model.film.util.FilmType
import com.flixclusive.model.film.util.extractYear
import com.flixclusive.model.film.DEFAULT_FILM_SOURCE_NAME
import com.flixclusive.model.film.FilmSearchItem
import com.flixclusive.model.film.Genre
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
    @SerializedName("vote_count") val voteCount: Int = 0,
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
        id = null,
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
        voteCount = voteCount,
        year = releaseDate?.extractYear(),
        genres = listOf(
            Genre(
                id = -1,
                name = genreName
            )
        ),
    )
}