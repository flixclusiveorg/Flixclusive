package com.flixclusive.core.database.entity.film

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.flixclusive.model.film.Film
import com.flixclusive.model.film.util.FilmType
import java.io.Serializable
import java.util.Date

/**
 * Represents a film entity in the database.
 *
 * Stores only stable identity fields. Mutable metadata like ratings, overviews,
 * and backdrops are intentionally excluded.
 * */
@Entity(tableName = "films")
data class DBFilm(
    @PrimaryKey
    val id: String,
    val title: String,
    val providerId: String,
    val filmType: FilmType = FilmType.MOVIE,
    val overview: String? = null,
    val posterImage: String? = null,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date(),
) : Serializable {
    companion object {
        fun Film.toDBFilm(): DBFilm = DBFilm(
            id = identifier,
            overview = overview,
            providerId = providerId,
            filmType = filmType,
            title = title,
            posterImage = posterImage,
        )
    }
}

/** Deprecated */
internal data class DBFilmV213(
    @PrimaryKey
    override val id: String,
    override val providerId: String = "",
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
    override val filmType: FilmType = FilmType.MOVIE,
    override val rating: Double? = null,
    override val customProperties: Map<String, String?> = emptyMap(),
    val hasRecommendations: Boolean = false,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date(),
) : Film(),
    Serializable {
    companion object {
        const val DB_FILM_VALID_RECOMMENDATIONS_COUNT = 3
    }
}
