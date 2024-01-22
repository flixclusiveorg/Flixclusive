package com.flixclusive.model.tmdb

import com.flixclusive.core.util.exception.safeCall
import com.flixclusive.core.util.film.FilmType
import com.flixclusive.core.util.film.isDateInFuture
import com.flixclusive.model.tmdb.util.formatDate
import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Serializable
sealed class TMDBSearchItem : Film {
    @Serializable
    data class MovieTMDBSearchItem(
        @SerializedName("backdrop_path") override val backdropImage: String? = null,
        @SerializedName("genre_ids") val genreIds: List<Int> = emptyList(),
        @SerializedName("original_language") override val language: String = "en",
        @SerializedName("poster_path") override val posterImage: String? = null,
        @SerializedName("release_date") val releaseDate: String = "",
        @SerializedName("vote_average") override val rating: Double = 0.0,
        override val genres: List<Genre> = listOf(Genre(-1, "Movie")),
        override val id: Int = 0,
        override val logoImage: String? = null,
        override val overview: String = "",
        override val title: String = "",
    ) : TMDBSearchItem() {
        override val filmType: FilmType
            get() = FilmType.MOVIE
        override val dateReleased: String
            get() = formatDate(releaseDate)
        override val isReleased: Boolean
            get() = if(releaseDate.isEmpty()) false else safeCall { !isDateInFuture(releaseDate) } ?: true
    }

    @Serializable
    data class TvShowTMDBSearchItem(
        @SerializedName("backdrop_path") override val backdropImage: String? = null,
        @SerializedName("first_air_date") val firstAirDate: String = "",
        @SerializedName("genre_ids") val genreIds: List<Int> = emptyList(),
        @SerializedName("name") override val title: String = "",
        @SerializedName("original_language") override val language: String = "en",
        @SerializedName("poster_path") override val posterImage: String? = null,
        @SerializedName("vote_average") override val rating: Double = 0.0,
        override val genres: List<Genre> = listOf(Genre(-1, "TV Show")),
        override val id: Int = 0,
        override val logoImage: String? = null,
        override val overview: String = "",
    ) : TMDBSearchItem() {
        override val filmType: FilmType
            get() = FilmType.TV_SHOW
        override val dateReleased: String
            get() = formatDate(firstAirDate)
        override val isReleased: Boolean
            get() = if(firstAirDate.isEmpty()) false else safeCall { !isDateInFuture(firstAirDate) } ?: true
    }
}

fun TMDBSearchItem.toRecommendation(): Recommendation {
    return Recommendation(
        id = id,
        title = title,
        image = posterImage,
        mediaType = when (filmType) {
            FilmType.MOVIE -> "Movie"
            FilmType.TV_SHOW -> "TV Series"
        },
        rating = rating,
        releaseDate = dateReleased,
        isReleased = isReleased
    )
}


