package com.flixclusive.core.database.entity.library

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.flixclusive.core.database.entity.user.User
import java.io.Serializable
import java.util.Date
import java.util.UUID

enum class LibraryListType {
    WATCHED,
    CUSTOM;

    val isWatched get() = this == WATCHED
}

@Entity(
    tableName = "library_lists",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["userId"],
            childColumns = ["ownerId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["ownerId"]),
        Index(value = ["ownerId", "name"], unique = true)
    ]
)
data class LibraryList(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val ownerId: String,
    val name: String,
    val description: String? = null,
    val listType: LibraryListType = LibraryListType.CUSTOM,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date(),
) : Serializable {
    val isCustom get() = listType == LibraryListType.CUSTOM
}
