package com.flixclusive.data.dto.tmdb

import com.flixclusive.data.dto.tmdb.common.ProductionCompany
import com.flixclusive.data.dto.tmdb.common.ProductionCountry
import com.flixclusive.data.dto.tmdb.common.SpokenLanguage
import com.flixclusive.data.dto.tmdb.common.TMDBImagesResponseDto
import com.flixclusive.data.dto.tmdb.tv.Creator
import com.flixclusive.data.dto.tmdb.tv.EpisodeAir
import com.flixclusive.data.dto.tmdb.tv.Network
import com.flixclusive.data.dto.tmdb.tv.TvShowSeasonsPreview
import com.flixclusive.data.dto.tmdb.tv.toSeason
import com.flixclusive.domain.model.tmdb.Genre
import com.flixclusive.domain.model.tmdb.TMDBPageResponse
import com.flixclusive.domain.model.tmdb.TMDBSearchItem
import com.flixclusive.domain.model.tmdb.TvShow
import com.flixclusive.domain.model.tmdb.toRecommendation
import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("tv_show")
data class TMDBTvShowDto(
    @SerializedName("backdrop_path") val backdropPath: String?,
    @SerializedName("created_by") val createdBy: List<Creator>,
    @SerializedName("episode_run_time") val episodeRunTime: List<Int>?,
    @SerializedName("first_air_date") val firstAirDate: String,
    val genres: List<Genre>,
    val homepage: String?,
    val id: Int,
    @SerializedName("in_production") val inProduction: Boolean,
    val languages: List<String>,
    @SerializedName("last_air_date") val lastAirDate: String?,
    @SerializedName("last_episode_to_air") val lastEpisodeToAir: EpisodeAir?,
    val name: String,
    @SerializedName("next_episode_to_air") val nextEpisodeToAir: EpisodeAir?,
    val networks: List<Network>,
    @SerializedName("number_of_episodes") val numberOfEpisodes: Int,
    @SerializedName("number_of_seasons") val numberOfSeasons: Int,
    @SerializedName("origin_country") val originCountry: List<String>,
    @SerializedName("original_language") val originalLanguage: String,
    @SerializedName("original_name") val originalName: String,
    val overview: String?,
    val popularity: Double,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("production_companies") val productionCompanies: List<ProductionCompany>,
    @SerializedName("production_countries") val productionCountries: List<ProductionCountry>,
    val seasons: List<TvShowSeasonsPreview>,
    @SerializedName("spoken_languages") val spokenLanguages: List<SpokenLanguage>,
    val status: String,
    val tagline: String?,
    val type: String,
    @SerializedName("vote_average") val voteAverage: Double,
    @SerializedName("vote_count") val voteCount: Int,
    val images: TMDBImagesResponseDto,
    val recommendations: TMDBPageResponse<TMDBSearchItem>
)

fun TMDBTvShowDto.toTvShow(): TvShow {
    val logoPath = try {
        images.logos[0].filePath
    } catch (e: Exception) {
        null
    }

    return TvShow(
        id = id,
        title = name,
        image = posterPath,
        cover = backdropPath,
        logo = logoPath,
        rating = voteAverage,
        releaseDate = firstAirDate,
        lastAirDate = lastAirDate,
        description = overview,
        genres = genres,
        duration = if(episodeRunTime?.isNotEmpty() == true) episodeRunTime[0] else null,
        totalEpisodes = numberOfEpisodes,
        totalSeasons = numberOfSeasons,
        recommendations = recommendations.results.map { it.toRecommendation() },
        inProduction = inProduction,
        language = originalLanguage,
        seasons = seasons.map { it.toSeason() }
    )
}