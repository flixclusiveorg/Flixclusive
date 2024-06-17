package com.flixclusive.model.tmdb.common.tv

import com.flixclusive.core.util.exception.safeCall
import com.flixclusive.model.tmdb.util.formatDate
import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Serializable
data class Episode(
    val id: String = "",
    val overview: String = "",
    val runtime: Int? = null,
    @SerializedName("episode_number") val number: Int = 0,
    @SerializedName("name") val title: String = "",
    @SerializedName("air_date") private val airDate: String? = null,
    @SerializedName("season_number") val season: Int = 0,
    @SerializedName("still_path") val image: String? = null,
    @SerializedName("vote_average") val rating: Double? = null
) : java.io.Serializable {
    val releaseDate: String
        get() = safeCall { formatDate(airDate) } ?: airDate ?: ""

    override fun hashCode(): Int {
        return super.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if(other !is Episode)
            return false

        return season == other.season && number == other.number && id == other.id
    }
}