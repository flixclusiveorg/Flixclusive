package com.flixclusive.core.network.retrofit.dto

import com.flixclusive.core.util.exception.safeCall
import com.flixclusive.core.util.film.extractYear
import com.flixclusive.model.tmdb.DEFAULT_FILM_SOURCE_NAME
import com.flixclusive.model.tmdb.FilmSearchItem
import com.flixclusive.model.tmdb.Genre
import com.flixclusive.model.tmdb.Person
import com.flixclusive.model.tmdb.SearchResponseData
import com.flixclusive.model.tmdb.TvShow
import com.flixclusive.model.tmdb.common.details.Company
import com.flixclusive.model.tmdb.common.tv.Season
import com.flixclusive.model.tmdb.util.filterOutUnreleasedFilms
import com.flixclusive.model.tmdb.util.formatAirDates
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
