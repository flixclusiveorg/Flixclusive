package com.flixclusive.core.network.retrofit.dto.tv

import com.flixclusive.core.util.exception.safeCall
import com.flixclusive.core.util.film.isDateInFuture
import com.flixclusive.model.tmdb.Season
import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Serializable
data class TvShowSeasonsPreview(
    @SerializedName("air_date") val airDate: String?,
    @SerializedName("episode_count") val episodeCount: Int,
    val id: Int,
    val name: String,
    val overview: String?,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("season_number") val seasonNumber: Int,
    @SerializedName("vote_average") val voteAverage: Double,
)

fun TvShowSeasonsPreview.toSeason(): Season {
    return Season(
        seasonNumber = seasonNumber,
        image = posterPath,
        episodes = emptyList(),
        isReleased = if(airDate != null) safeCall { !isDateInFuture(airDate) } ?: true else false
    )
}