package com.flixclusive.provider.lookmovie.dto

import com.flixclusive.model.provider.Subtitle
import com.flixclusive.provider.base.dto.FilmInfo
import com.flixclusive.provider.base.util.toValidReleaseDate
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
       val id: Int,
       val episode: Int,
       val season: Int,
    )

    companion object {
        fun LookMovieMediaDetail.toMediaInfo() = FilmInfo(
            id = id.toString(),
            title = title,
            yearReleased = releaseDate.toValidReleaseDate("yyyy-MM-dd HH:mm:ss")?.split("-")?.first() ?: year.toString()
        )
    }
}