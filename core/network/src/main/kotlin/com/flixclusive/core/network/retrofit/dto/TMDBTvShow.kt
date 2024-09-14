package com.flixclusive.core.network.retrofit.dto

import com.flixclusive.core.util.exception.safeCall
import com.flixclusive.model.film.DEFAULT_FILM_SOURCE_NAME
import com.flixclusive.model.film.FilmSearchItem
import com.flixclusive.model.film.Genre
import com.flixclusive.model.film.Person
import com.flixclusive.model.film.SearchResponseData
import com.flixclusive.model.film.TvShow
import com.flixclusive.model.film.common.details.Company
import com.flixclusive.model.film.common.tv.Season
import com.flixclusive.model.film.util.extractYear
import com.flixclusive.model.film.util.filterOutUnreleasedFilms
import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Serializable
internal data class TMDBTvShow(
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
    @SerializedName("number_of_episodes") val totalEpisodes: Int = 0,
    @SerializedName("number_of_seasons") val totalSeasons: Int = 0,
    @SerializedName("last_air_date") val lastAirDate: String? = null,
    @SerializedName("in_production") val inProduction: Boolean? = null,
    @SerializedName("episode_run_time") val episodeRuntime: List<Int> = emptyList(),
    @SerializedName("tagline") val tagLine: String? = null,
    val adult: Boolean = false,
    val overview: String? = null,
    val networks: List<Company> = emptyList(),
    val seasons: List<Season> = emptyList(),
    val credits: Map<String, List<Person>> = emptyMap(),
    val genres: List<Genre> = emptyList(),
    val recommendations: SearchResponseData<FilmSearchItem> = SearchResponseData(),
    val images: TMDBImagesResponseDto = TMDBImagesResponseDto(),
)

internal fun TMDBTvShow.toTvShowDetails(): TvShow {
    return TvShow(
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
        runtime = episodeRuntime.average().toInt(),
        overview = overview,
        tagLine = tagLine,
        year = releaseDate?.extractYear(),
        genres = genres,
        cast = credits["cast"] ?: emptyList(),
        networks = networks,
        seasons = seasons,
        parsedReleaseDate = safeCall {
            formatAirDates(
                firstAirDate = releaseDate!!,
                lastAirDate = lastAirDate!!,
                inProduction = inProduction
            )
        } ?: releaseDate,
        totalEpisodes = totalEpisodes,
        totalSeasons = totalSeasons,
    )
}

private fun formatAirDates(
    firstAirDate: String,
    lastAirDate: String,
    inProduction: Boolean?,
): String {
    if (firstAirDate.isEmpty() && lastAirDate.isEmpty()) {
        return "No air dates"
    }

    val firstYear = firstAirDate.substring(0, 4)
    val lastYear = if (inProduction == true) "present" else lastAirDate.substring(0, 4)

    return "$firstYear-$lastYear"
}
