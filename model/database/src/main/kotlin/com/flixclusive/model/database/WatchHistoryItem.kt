package com.flixclusive.model.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.flixclusive.model.film.DBFilm
import com.flixclusive.model.film.Film
import com.flixclusive.model.film.TvShow
import com.flixclusive.model.film.toFilmInstance
import java.io.Serializable
import java.util.Date

@Entity(tableName = "watch_history")
data class WatchHistoryItem(
    @PrimaryKey val id: String = "",
    val ownerId: Int = 1,
    val seasons: Int? = null,
    val episodes: Map<Int, Int> = emptyMap(),
    val episodesWatched: List<EpisodeWatched> = emptyList(),
    val dateWatched: Date = Date(),
    val film: DBFilm = DBFilm()
) : Serializable

data class EpisodeWatched(
    val episodeId: String = "",
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
    val watchTime: Long = 0,
    val durationTime: Long = 0,
    val isFinished: Boolean = false
) : Serializable


fun Film.toWatchHistoryItem(): WatchHistoryItem {
    val seasonCount = when(this is TvShow) {
        true -> totalSeasons
        false -> null
    }

    return WatchHistoryItem(
        id = identifier,
        seasons = seasonCount,
        film = this.toFilmInstance()
    )
}