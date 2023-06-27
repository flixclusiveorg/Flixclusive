package com.flixclusive.domain.model.tmdb

import com.flixclusive.presentation.common.Formatter.formatDate
import com.flixclusive.presentation.common.Formatter.formatMinutes
import kotlinx.serialization.Serializable

@Serializable
data class Movie(
    override val id: Int = -1,
    override val title: String = "",
    val image: String? = null,
    val cover: String? = null,
    val logo: String? = null,
    val type: String = "Movie",
    override val rating: Double = 0.0,
    val releaseDate: String = "",
    val description: String? = null,
    val genresList: List<Genre> = emptyList(),
    val duration: Int? = null,
    val recommendations: List<Recommendation> = emptyList()
) : Film, java.io.Serializable {
    override val filmType: FilmType
        get() = FilmType.MOVIE

    override val posterImage: String?
        get() = image

    override val dateReleased: String
        get() = formatDate(releaseDate)

    override val runtime: String
        get() = formatMinutes(duration)

    override val overview: String?
        get() = description

    override val genres: List<Genre>
        get() = genresList.map { it.copy(mediaType = filmType.type) }

    override val backdropImage: String?
        get() = cover

    override val logoImage: String?
        get() = logo

    override val recommendedTitles: List<Recommendation>
        get() = recommendations
}


