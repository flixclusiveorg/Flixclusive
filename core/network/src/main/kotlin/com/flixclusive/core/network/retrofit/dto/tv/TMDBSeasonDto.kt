package com.flixclusive.core.network.retrofit.dto.tv

import com.flixclusive.core.util.exception.safeCall
import com.flixclusive.core.util.film.isDateInFuture
import com.flixclusive.model.tmdb.Season
import com.google.gson.annotations.SerializedName

data class TMDBSeasonDto(
    val _id: String,
    @SerializedName("air_date") val airDate: String?,
    val episodes: List<TMDBEpisodeDto>,
    val name: String,
    val overview: String,
    val id: Int,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("season_number") val seasonNumber: Int
)

fun TMDBSeasonDto.toSeason(): Season {
    return Season(
        seasonNumber = seasonNumber,
        image = posterPath,
        name = name,
        episodes = episodes.map { it.toEpisode() },
        isReleased = if(airDate != null) safeCall { !isDateInFuture(airDate) } ?: true else false
    )
}