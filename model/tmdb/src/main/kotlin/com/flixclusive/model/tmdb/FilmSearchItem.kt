package com.flixclusive.model.tmdb

import com.flixclusive.core.util.film.FilmType
import kotlinx.serialization.Serializable

/**
 * Represents a film search result item.
 *
 * @property id The unique identifier for the film.
 * @property title The title of the film.
 * @property providerName The name of the provider where this film came from.
 * @property homePage The URL of the film's home page.
 * @property posterImage The URL of the film's poster image.
 * @property filmType The type of the film. Defaults to [FilmType.MOVIE]. For more, see [FilmType].
 * @property backdropImage The URL of the film's backdrop image.
 * @property tmdbId The TMDB ID of the film.
 * @property rating The rating of the film. Defaults to 0.0.
 * @property language The language of the film.
 * @property adult Whether the film is marked as adult. Defaults to false.
 * @property overview The overview of the film.
 * @property releaseDate The release date of the film.
 * @property year The release year of the film.
 * @property logoImage The URL of the film's logo image.
 * @property genres A list of genres associated with the film.
 * @property genreIds A list of genre IDs associated with the film.
 *
 * @see Film
 */
@Serializable
data class FilmSearchItem(
    override val id: String?,
    override val providerName: String,
    /** @see FilmType */
    override val filmType: FilmType,
    override val homePage: String?,
    override val title: String,
    override val posterImage: String?,
    override val adult: Boolean = false,
    override val backdropImage: String? = null,
    override val imdbId: String? = null,
    override val tmdbId: Int? = null,
    override val releaseDate: String? = null,
    override val rating: Double? = null,
    override val language: String? = null,
    override val overview: String? = null,
    override val year: Int? = null,
    override val logoImage: String? = null,
    override val genres: List<Genre> = emptyList(),
    val genreIds: List<Int> = emptyList(),
) : Film()