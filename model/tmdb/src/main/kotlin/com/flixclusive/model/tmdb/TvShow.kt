package com.flixclusive.model.tmdb

import com.flixclusive.core.util.film.FilmType
import com.flixclusive.core.util.film.formatMinutes
import com.flixclusive.model.tmdb.util.formatAirDates
import kotlinx.serialization.Serializable

@Serializable
data class TvShow(
    override val id: Int = -1,
    override val title: String = "",
    val image: String? = null,
    val cover: String? = null,
    val logo: String? = null,
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
        get() = formatAirDates(
            firstAirDate = releaseDate,
            lastAirDate = lastAirDate ?: "",
            inProduction = inProduction
        )

    override val runtime: String
        get() {
            var runtimeString = ""

            if (duration != null)
                runtimeString = "${formatMinutes(duration)} | "

            if(totalSeasons > 0) {
                runtimeString += "$totalSeasons Season"

                if(totalSeasons > 1)
                    runtimeString += "s"
            }


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
