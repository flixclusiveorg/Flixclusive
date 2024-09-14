package com.flixclusive.core.network.retrofit.dto

import com.flixclusive.model.film.util.extractYear
import com.flixclusive.model.film.DEFAULT_FILM_SOURCE_NAME
import com.flixclusive.model.film.FilmSearchItem
import com.flixclusive.model.film.Genre
import com.flixclusive.model.film.Movie
import com.flixclusive.model.film.Person
import com.flixclusive.model.film.SearchResponseData
import com.flixclusive.model.film.TMDBCollection
import com.flixclusive.model.film.common.details.Company
import com.flixclusive.model.film.util.filterOutUnreleasedFilms
import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Serializable
internal data class TMDBMovie(
    @SerializedName("title", alternate = ["name"]) val title: String,
    @SerializedName("id") val tmdbId: Int? = null,
    @SerializedName("poster_path") val posterImage: String? = null,
    @SerializedName("backdrop_path") val backdropImage: String? = null,
    @SerializedName("homepage") val homePage: String? = null,
    @SerializedName("original_language") val language: String? = null,
    @SerializedName("vote_average") val rating: Double? = null,
    @SerializedName("production_companies") val producers: List<Company> = emptyList(),
    @SerializedName("external_ids") val externalIds: Map<String, String?> = emptyMap(),
    @SerializedName("belongs_to_collection") val belongsToCollection: BelongsToCollection? = null,
    @SerializedName("imdb_id") val imdbId: String? = null,
    @SerializedName("release_date", alternate = ["first_air_date"]) val releaseDate: String? = null,
    @SerializedName("tagline") val tagLine: String? = null,
    val credits: Map<String, List<Person>> = emptyMap(),
    val adult: Boolean = false,
    val genres: List<Genre> = emptyList(),
    val overview: String? = null,
    val runtime: Int? = null,
    val recommendations: SearchResponseData<FilmSearchItem> = SearchResponseData(),
    val images: TMDBImagesResponseDto = TMDBImagesResponseDto(),
)

@Serializable
internal data class BelongsToCollection(
    val id: Int,
    val name: String,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("backdrop_path") val backdropPath: String?
)

internal fun TMDBMovie.toMovieDetails(): Movie {
    return Movie(
        tmdbId = tmdbId,
        title = title,
        posterImage = posterImage,
        backdropImage = backdropImage,
        logoImage = images.logos?.firstOrNull()?.filePath?.replace("svg", "png"),
        homePage = homePage,
        language = language,
        releaseDate = releaseDate,
        rating = rating,
        producers = producers,
        recommendations = recommendations.results.filterOutUnreleasedFilms(),
        id = null,
        providerName = DEFAULT_FILM_SOURCE_NAME,
        imdbId = imdbId ?: externalIds["imdb_id"],
        adult = adult,
        runtime = runtime,
        overview = overview,
        tagLine = tagLine,
        year = releaseDate?.extractYear(),
        genres = genres,
        cast = credits["cast"] ?: emptyList(),
        collection = belongsToCollection?.run {
            TMDBCollection(
                id = id,
                collectionName = name,
                posterPath = posterPath,
                backdropPath = backdropPath,
                films = emptyList(),
            )
        },
    )
}
