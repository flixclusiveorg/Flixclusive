package com.flixclusive.providers.models.providers.lookmovie

import com.flixclusive.providers.models.common.MediaInfo
import com.flixclusive.providers.models.common.Subtitle
import com.flixclusive.providers.utils.toValidReleaseDate
import com.google.gson.annotations.SerializedName

data class LookMovieMediaDetail(
    val episodes: List<Episode>? = null,
    val streams: Map<String, String>? = null,
    val subtitles: List<Subtitle>? = null,
    val title: String = "",
    val year: Int? = null,
    @SerializedName("id_show", alternate = ["id_movie"]) val id: Int,
    @SerializedName("first_air_date", alternate = ["release_date"]) val releaseDate: String? = null
) {
   data class Episode(
        val description: String,
        val episode: Int,
        val id: Int,
        val id_show: Int,
        val imdb_rating: Any,
        val is_active: Int,
        val poster: String,
        val rel_title: String,
        val season: Int,
        val shard: String,
        val show_title: Any,
        val still_path: String,
        val title: String
    )

    companion object {
        fun LookMovieMediaDetail.toMediaInfo() = MediaInfo(
            id = id.toString(),
            title = title,
            releaseDate = releaseDate.toValidReleaseDate("yyyy-MM-dd HH:mm:ss")?.split("-")?.first() ?: year.toString()
        )
    }
}