package com.flixclusive.core.database.entity.film

import androidx.room.Entity
import androidx.room.Ignore
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
    override val id: String,
    override val title: String,
    override val providerId: String,
    override val adult: Boolean,
    override val filmType: FilmType,
    override val overview: String?,
    override val posterImage: String?,
    override val language: String?,
    override val rating: Double?,
    override val backdropImage: String?,
    override val releaseDate: String?,
    override val year: Int?,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date(),
) : Serializable, Film() {
    override val homePage get() = null
    override val customProperties: Map<String, String?> get() = emptyMap()

    companion object {
        fun Film.toDBFilm(): DBFilm = DBFilm(
            id = identifier,
            adult = adult,
            overview = overview,
            providerId = providerId,
            filmType = filmType,
            title = title,
            posterImage = posterImage,
            language = language,
            rating = rating,
            backdropImage = backdropImage,
            releaseDate = releaseDate,
            year = year,
        )
    }
}

@Deprecated("Only used for migration from v2.1.3, should be removed after migration is complete")
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
