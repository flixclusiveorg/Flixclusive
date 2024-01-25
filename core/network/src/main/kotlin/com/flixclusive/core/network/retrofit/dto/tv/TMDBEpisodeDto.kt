package com.flixclusive.core.network.retrofit.dto.tv

import com.flixclusive.model.tmdb.TMDBEpisode
import com.google.gson.annotations.SerializedName

data class TMDBEpisodeDto(
    @SerializedName("air_date") val airDate: String?,
    @SerializedName("episode_number") val episodeNumber: Int,
    val id: Int,
    val name: String,
    val overview: String,
    @SerializedName("production_code") val productionCode: String,
    val runtime: Int?,
    @SerializedName("season_number") val seasonNumber: Int,
    @SerializedName("show_id") val showId: Int ,
    @SerializedName("still_path") val stillPath: String?,
    @SerializedName("vote_average") val voteAverage: Double,
    @SerializedName("vote_count") val voteCount: Int,
    val crew: List<CrewMember>,
    @SerializedName("guest_stars") val guestStars: List<GuestStar>,
)

fun TMDBEpisodeDto.toEpisode(): TMDBEpisode {
    return TMDBEpisode(
        episodeId = id,
        title = name,
        episode = episodeNumber,
        season = seasonNumber,
        releaseDate = airDate ?: "",
        description = overview,
        image = stillPath,
        rating = voteAverage
    )
}