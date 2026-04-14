package com.flixclusive.core.database.entity.library

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.flixclusive.core.database.entity.film.DBFilm
import java.util.Date

@Entity(
    tableName = "library_list_items",
    foreignKeys = [
        ForeignKey(
            entity = LibraryList::class,
            parentColumns = ["id"],
            childColumns = ["listId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = DBFilm::class,
            parentColumns = ["id"],
            childColumns = ["filmId"],
        ),
    ],
    indices = [
        Index(value = ["filmId", "listId"], unique = true),
        Index(value = ["filmId"]),
        Index(value = ["listId"]),
    ],
)
data class LibraryListItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val filmId: String,
    val listId: Int,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date(),
)
