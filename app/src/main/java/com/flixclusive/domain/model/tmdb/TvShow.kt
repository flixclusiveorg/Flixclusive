package com.flixclusive.domain.model.tmdb

import com.flixclusive.presentation.utils.FormatterUtils
import kotlinx.serialization.Serializable

@Serializable
data class TvShow(
    override val id: Int = -1,
    override val title: String = "",
    val image: String? = null,
    val cover: String? = null,
    val logo: String? = null,
    val type: String = "TV Series",
    override val rating: Double = 0.0,
    val releaseDate: String = "",
    val lastAirDate: String? = null,
    val description: String? = null,
    override val language: String,
    override val genres: List<Genre> = emptyList(),
    val duration: Int? = null,
    val totalEpisodes: Int = 0,
    val totalSeasons: Int = 0,
    val recommendations: List<Recommendation> = emptyList(),
    val seasons: List<Season> = emptyList(),
    val inProduction: Boolean? = null,
) : Film, java.io.Serializable {
    override val filmType: FilmType
        get() = FilmType.TV_SHOW

    override val posterImage: String?
        get() = image

    override val dateReleased: String
        get() = FormatterUtils.formatAirDates(
            releaseDate,
            lastAirDate ?: "",
            inProduction
        )

    override val runtime: String
        get() {
            var runtimeString = when {
                duration != null -> "${FormatterUtils.formatMinutes(duration)} | $totalSeasons Season"
                else -> "$totalSeasons Season"
            }

            if(totalSeasons > 1)
                runtimeString += "s"

            if(totalEpisodes > 0) {
                runtimeString += " | $totalEpisodes Episodes"

                if(totalEpisodes > 1)
                    runtimeString += "s"
            }

            return runtimeString
        }

    override val overview: String?
        get() = description

    override val backdropImage: String?
        get() = cover

    override val logoImage: String?
        get() = logo

    override val recommendedTitles: List<Recommendation>
        get() = recommendations
}
