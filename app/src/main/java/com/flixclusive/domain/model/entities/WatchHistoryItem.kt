package com.flixclusive.domain.model.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.flixclusive.domain.model.tmdb.Film
import com.flixclusive.domain.model.tmdb.FilmImpl
import com.flixclusive.domain.model.tmdb.TvShow
import com.flixclusive.domain.model.tmdb.toFilmInstance
import java.io.Serializable
import java.util.Date

@Entity(tableName = "watch_history")
data class WatchHistoryItem(
    @PrimaryKey val id: Int = 0,
    val ownerId: Int = 1,
    val seasons: Int? = null,
    val episodes: Map<Int, Int> = emptyMap(),
    val episodesWatched: List<EpisodeWatched> = emptyList(),
    val dateWatched: Date = Date(),
    val film: FilmImpl = FilmImpl()
) : Serializable

data class EpisodeWatched(
    val episodeId: Int = 0,
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
        id = id,
        seasons = seasonCount,
        film = this.toFilmInstance()
    )
}