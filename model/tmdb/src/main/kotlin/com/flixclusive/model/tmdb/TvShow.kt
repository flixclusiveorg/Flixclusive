package com.flixclusive.model.tmdb

import com.flixclusive.core.util.film.FilmType
import com.flixclusive.model.tmdb.common.details.Company
import com.flixclusive.model.tmdb.common.tv.Season
import kotlinx.serialization.Serializable

/**
 * Represents a detailed information of a TV show.
 *
 * @property id The unique identifier of the TV show.
 * @property title The title of the TV show.
 * @property providerName The name of the provider where this film came from.
 * @property homePage The home page of the TV show.
 * @property posterImage The poster image of the TV show.
 * @property backdropImage The backdrop image of the TV show.
 * @property tmdbId The TMDB ID of the TV show.
 * @property rating The rating of the TV show.
 * @property language The language of the TV show.
 * @property adult Whether the TV show is marked as adult.
 * @property overview The overview of the TV show.
 * @property releaseDate The release date of the TV show.
 * @property tagLine The tag line of the TV show.
 * @property year The year of the TV show.
 * @property producers The producers of the TV show.
 * @property genres The genres of the TV show.
 * @property networks The networks that aired the TV show.
 * @property seasons The seasons of the TV show.
 * @property totalEpisodes The total number of episodes in the TV show.
 * @property totalSeasons The total number of seasons in the TV show.
 * @property runtime The runtime of the TV show.
 * @property filmType The type of the TV show.
 * @property parsedReleaseDate The parsed release date of the TV show.
 * @property releaseStatus The release status of the TV show.
 * @property customProperties A map of custom properties associated with the film. Add any properties that your response/resource needs. Also, serialize the value of the property to string.
 *
 * @see FilmDetails
 * @see Film
 * @see Movie
 * */
@Serializable
data class TvShow(
    override val id: String?,
    override val title: String,
    override val posterImage: String?,
    override val homePage: String?,
    override val backdropImage: String? = null,
    override val logoImage: String? = null,
    override val tmdbId: Int? = null,
    override val imdbId: String? = null,
    override val language: String? = null,
    override val releaseDate: String? = null,
    override val parsedReleaseDate: String? = null,
    override val rating: Double? = null,
    override val producers: List<Company> = emptyList(),
    override val recommendations: List<FilmSearchItem> = emptyList(),
    override val providerName: String?,
    override val adult: Boolean = false,
    override val overview: String? = null,
    override val tagLine: String? = null,
    override val year: Int? = null,
    override val genres: List<Genre> = emptyList(),
    override val cast: List<Person> = emptyList(),
    override val customProperties: Map<String, String?> = emptyMap(),

    // == Custom fields ==
    val networks: List<Company> = emptyList(),
    val seasons: List<Season> = emptyList(),
    val totalEpisodes: Int = 0,
    val totalSeasons: Int = 0,
    override val runtime: Int? = null,
) : FilmDetails() {
    override val filmType: FilmType
        get() = FilmType.TV_SHOW
}
