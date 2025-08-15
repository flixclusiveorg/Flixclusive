package com.flixclusive.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.flixclusive.model.film.Film
import com.flixclusive.model.film.TvShow
import java.io.Serializable
import java.util.Date

// TODO: Refactor this entity to use a more appropriate structure for Movies and TV Shows
//       Consider separating them into distinct entities or using a polymorphic approach.
@Entity(tableName = "watch_history")
data class WatchHistory(
    @PrimaryKey override val id: String,
    override val film: DBFilm,
    val ownerId: Int,
    val seasons: Int? = null,
    val episodes: Map<Int, Int> = emptyMap(),
    val episodesWatched: List<EpisodeWatched> = emptyList(),
    val dateWatched: Date = Date(),
) : Serializable,
    DBFilmItem

data class EpisodeWatched(
    val episodeId: String = "",
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
    val watchTime: Long = 0,
    val durationTime: Long = 0,
    val isFinished: Boolean = false,
) : Serializable

fun Film.toWatchHistoryItem(ownerId: Int): WatchHistory {
    val seasonCount =
        when (this is TvShow) {
            true -> totalSeasons
            false -> null
        }

    return WatchHistory(
        id = identifier,
        seasons = seasonCount,
        ownerId = ownerId,
        film = this.toDBFilm(),
    )
}
