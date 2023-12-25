package com.flixclusive.domain.model.tmdb

import com.flixclusive.presentation.utils.FormatterUtils.formatDate
import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Serializable
sealed class TMDBSearchItem : Film {
    @Serializable
    data class MovieTMDBSearchItem(
        @SerializedName("poster_path") override val posterImage: String? = null,
        override val overview: String = "",
        @SerializedName("release_date") val releaseDate: String = "",
        @SerializedName("genre_ids") val genreIds: List<Int> = emptyList(),
        override val id: Int = 0,
        @SerializedName("media_type") val mediaType: String? = null,
        @SerializedName("original_language") override val language: String = "en",
        override val title: String = "",
        @SerializedName("backdrop_path") override val backdropImage: String? = null,
        @SerializedName("vote_average") override val rating: Double = 0.0,
        override val logoImage: String? = null,
        override val genres: List<Genre> = listOf(Genre(-1, "Movie"))
    ) : TMDBSearchItem() {
        override val filmType: FilmType
            get() = FilmType.MOVIE
        override val dateReleased: String
            get() = formatDate(releaseDate)
        override val runtime: String
            get() = "0"
        override val recommendedTitles: List<Recommendation>
            get() = emptyList()
    }

    @Serializable
    data class TvShowTMDBSearchItem(
        @SerializedName("poster_path") override val posterImage: String? = null,
        override val id: Int = 0,
        override val overview: String = "",
        @SerializedName("backdrop_path") override val backdropImage: String? = null,
        @SerializedName("vote_average") override val rating: Double = 0.0,
        @SerializedName("media_type") val mediaType: String? = null,
        @SerializedName("first_air_date") val firstAirDate: String = "",
        @SerializedName("genre_ids") val genreIds: List<Int> = emptyList(),
        @SerializedName("original_language") override val language: String = "en",
        @SerializedName("name") override val title: String = "",
        override val logoImage: String? = null,
        override val genres: List<Genre> = listOf(Genre(-1, "TV Show"))
    ) : TMDBSearchItem() {
        override val filmType: FilmType
            get() = FilmType.TV_SHOW
        override val dateReleased: String
            get() = formatDate(firstAirDate)
        override val runtime: String
            get() = "0"
        override val recommendedTitles: List<Recommendation>
            get() = emptyList()
    }

    @Serializable
    data class PersonTMDBSearchItem(
        @SerializedName("profile_path") val profilePath: String = "",
        val adult: Boolean = false,
        @SerializedName("id") val _id: Int = 0,
        @SerializedName("media_type") val _mediaType: String? = null,
        @SerializedName("known_for") val knownFor: List<TMDBSearchItem> = emptyList(),
        val name: String = "",
        val popularity: Double = 0.0,
    ) : TMDBSearchItem() {
        override val language: String
            get() = "en"

        override val id: Int
            get() = -1
        override val title: String
            get() = ""
        override val posterImage: String?
            get() = null
        override val overview: String?
            get() = null
        override val filmType: FilmType
            get() = FilmType.MOVIE
        override val dateReleased: String
            get() = ""
        override val runtime: String
            get() = ""
        override val rating: Double
            get() = 0.0
        override val genres: List<Genre>
            get() = listOf()
        override val backdropImage: String?
            get() = null
        override val logoImage: String?
            get() = null
        override val recommendedTitles: List<Recommendation>
            get() = emptyList()
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
        releaseDate = dateReleased
    )
}


