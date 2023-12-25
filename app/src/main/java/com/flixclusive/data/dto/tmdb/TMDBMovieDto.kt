package com.flixclusive.data.dto.tmdb

import com.flixclusive.data.dto.tmdb.common.BelongsToCollection
import com.flixclusive.data.dto.tmdb.common.ProductionCompany
import com.flixclusive.data.dto.tmdb.common.ProductionCountry
import com.flixclusive.data.dto.tmdb.common.SpokenLanguage
import com.flixclusive.data.dto.tmdb.common.TMDBImagesResponseDto
import com.flixclusive.domain.model.tmdb.Genre
import com.flixclusive.domain.model.tmdb.Movie
import com.flixclusive.domain.model.tmdb.TMDBPageResponse
import com.flixclusive.domain.model.tmdb.TMDBSearchItem
import com.flixclusive.domain.model.tmdb.toRecommendation
import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("movie")
data class TMDBMovieDto(
    val adult: Boolean,
    @SerializedName("backdrop_path") val backdropPath: String?,
    @SerializedName("belongs_to_collection") val belongsToCollection: BelongsToCollection?,
    val budget: Int,
    val genres: List<Genre>,
    val homepage: String?,
    val id: Int,
    @SerializedName("imdb_id") val imdbId: String?,
    @SerializedName("original_language") val originalLanguage: String,
    @SerializedName("original_title") val originalTitle: String,
    val overview: String?,
    val popularity: Double,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("production_companies") val productionCompanies: List<ProductionCompany>,
    @SerializedName("production_countries") val productionCountries: List<ProductionCountry>,
    @SerializedName("release_date") val releaseDate: String,
    val revenue: Long,
    val runtime: Int?,
    @SerializedName("spoken_languages") val spokenLanguages: List<SpokenLanguage>,
    val status: String,
    val tagline: String?,
    val title: String,
    val video: Boolean,
    @SerializedName("vote_average") val voteAverage: Double,
    @SerializedName("vote_count") val voteCount: Int,
    val images: TMDBImagesResponseDto,
    val recommendations: TMDBPageResponse<TMDBSearchItem>
)

fun TMDBMovieDto.toMovie(): Movie {
    val logoPath = try {
        images.logos[0].filePath
    } catch (e: Exception) {
        null
    }

    return Movie(
        id = id,
        title = title,
        image = posterPath,
        cover = backdropPath,
        logo = logoPath,
        rating = voteAverage,
        releaseDate = releaseDate,
        description = overview,
        duration = runtime,
        genres = genres,
        language = originalLanguage,
        recommendations = recommendations.results.map { it.toRecommendation() }
    )
}


