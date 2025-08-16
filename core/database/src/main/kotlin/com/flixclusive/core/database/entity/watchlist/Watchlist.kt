package com.flixclusive.core.database.entity.watchlist

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.flixclusive.core.database.entity.ListItem
import com.flixclusive.core.database.entity.film.DBFilm
import com.flixclusive.core.database.entity.user.User
import java.util.Date

@Entity(
    tableName = "watchlist",
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
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["filmId", "ownerId"], unique = true),
        Index(value = ["filmId"]),
        Index(value = ["ownerId"]),
    ],
)
data class Watchlist(
    @PrimaryKey(autoGenerate = true)
    override val id: Long = 0,
    override val filmId: String,
    val ownerId: Int,
    val addedAt: Date = Date(),
) : ListItem
