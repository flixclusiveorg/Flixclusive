package com.flixclusive.core.database.entity.watched

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.flixclusive.core.database.entity.ListItem
import com.flixclusive.core.database.entity.film.DBFilm
import com.flixclusive.core.database.entity.user.User
import java.io.Serializable
import java.util.Date

/**
 * Represents a progress item for an episode in a series watch history.
 *
 * This class extends [WatchProgress] and includes additional fields
 * for season and episode numbers, allowing tracking of progress
 * for specific episodes within a series.
 * */
@Entity(
    tableName = "series_watch_history",
    foreignKeys = [
        ForeignKey(
            entity = DBFilm::class,
            parentColumns = ["id"],
            childColumns = ["filmId"],
        ),
        ForeignKey(
            entity = User::class,
            parentColumns = ["userId"],
            childColumns = ["ownerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["filmId", "ownerId", "seasonNumber", "episodeNumber"], unique = true),
        Index(value = ["filmId"]),
        Index(value = ["ownerId"]),
    ],
)
data class EpisodeProgress(
    @PrimaryKey(autoGenerate = true)
    override val id: Long = 0,
    override val filmId: String,
    override val ownerId: Int,
    override val progress: Long,
    override val duration: Long = 0,
    override val status: WatchStatus,
    override val watchedAt: Date = Date(),
    val seasonNumber: Int,
    val episodeNumber: Int,
) : Serializable,
    ListItem,
    WatchProgress
