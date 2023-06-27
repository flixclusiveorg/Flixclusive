package com.flixclusive.data.dto.tmdb.tv

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Serializable
data class EpisodeAir(
    @SerializedName("air_date") val airDate: String,
    @SerializedName("episode_number") val episodeNumber: Int,
    val id: Int,
    val name: String,
    val overview: String,
    @SerializedName("production_code") val productionCode: String?,
    @SerializedName("season_number") val seasonNumber: Int,
    @SerializedName("still_path") val stillPath: String?,
    @SerializedName("vote_average") val voteAverage: Double,
    @SerializedName("vote_count") val voteCount: Int,
)

