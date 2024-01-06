package com.flixclusive.provider.superstream.dto

import com.flixclusive.provider.base.dto.FilmInfo
import com.flixclusive.provider.base.util.toValidReleaseDate
import com.google.gson.annotations.SerializedName


internal data class SuperStreamMediaDetailResponse(
    val code: Int? = null,
    val msg: String? = null,
    val data: MediaData? = null,
) {
    data class MediaData(
        val id: Int? = null,
        val title: String? = null,
        val year: Int? = null,
        val released: String? = null,
        @SerializedName("max_season") val maxSeason: Int? = null,
        @SerializedName("max_episode") val maxEpisode: Int? = null,
    )

    companion object {
        fun SuperStreamMediaDetailResponse.toMediaInfo(isMovie: Boolean): FilmInfo {
            return FilmInfo(
                id = data?.id.toString(),
                title = data?.title
                    ?: throw NullPointerException("Movie title should not be blank or null!"),
                yearReleased = if (isMovie) data.released!!.toValidReleaseDate()!!
                else data.year.toString(),
                seasons = data.maxSeason,
                episodes = data.maxEpisode,
            )
        }
    }
}
