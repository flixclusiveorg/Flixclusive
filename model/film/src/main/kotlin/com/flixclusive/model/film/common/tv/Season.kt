package com.flixclusive.model.film.common.tv

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Serializable
data class Season(
    val overview: String? = null,
    val name: String = "",
    val episodes: List<Episode> = emptyList(),
    @SerializedName("air_date") private val airDate: String? = null,
    @SerializedName("episode_count") private val episodeCount: Int? = null,
    @SerializedName("vote_average") val rating: Double? = null,
    @SerializedName("season_number") val number: Int = 0,
    @SerializedName("poster_path") val image: String? = null,
) : java.io.Serializable {
    val isUnreleased: Boolean
        get() = airDate == null && episodeCount == 0 && rating == 0.0
}

