package com.flixclusive.core.database.entity.search

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.flixclusive.core.database.entity.user.User
import java.util.Date

@Entity(
    tableName = "search_history",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["userId"],
            childColumns = ["ownerId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["query", "ownerId"], unique = true),
        Index(value = ["ownerId"]),
    ],
)
data class SearchHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val query: String,
    val ownerId: Int,
    val searchedOn: Date = Date(),
)
