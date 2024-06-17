package com.flixclusive.model.tmdb.common.tv

import com.flixclusive.core.util.exception.safeCall
import com.flixclusive.core.util.film.isDateInFuture
import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Serializable
data class Season(
    val overview: String? = null,
    val name: String = "",
    val episodes: List<Episode> = emptyList(),
    @SerializedName("air_date") internal val airDate: String? = null,
    @SerializedName("episode_count") internal val episodeCount: Int? = null,
    @SerializedName("vote_average") val rating: Double? = null,
    @SerializedName("season_number") val number: Int = 0,
    @SerializedName("poster_path") val image: String? = null,
) : java.io.Serializable {
    val isReleased: Boolean
        get() = if(airDate != null) {
            safeCall { !isDateInFuture(airDate) } ?: true
        } else false
}

