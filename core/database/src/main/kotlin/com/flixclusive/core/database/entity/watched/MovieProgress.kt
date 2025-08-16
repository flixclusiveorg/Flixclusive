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
 * Represents a movie progress item in the watch history.
 *
 * This entity extends [WatchProgress] and includes the film ID, owner ID,
 * progress in seconds, status of the watch, duration of the film in seconds,
 * the date when it was watched, and the watch count.
 * */
@Entity(
    tableName = "movies_watch_history",
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
        Index(value = ["filmId", "ownerId"], unique = true),
        Index(value = ["filmId"]),
        Index(value = ["ownerId"]),
    ],
)
data class MovieProgress(
    @PrimaryKey(autoGenerate = true)
    override val id: Long = 0,
    override val filmId: String,
    override val ownerId: Int,
    override val progress: Long,
    override val status: WatchStatus,
    override val duration: Long = 0,
    override val watchedAt: Date = Date(),
    val watchCount: Int = 1,
) : Serializable,
    ListItem,
    WatchProgress
