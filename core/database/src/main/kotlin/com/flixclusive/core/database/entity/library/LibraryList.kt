package com.flixclusive.core.database.entity.library

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.flixclusive.core.database.entity.user.User
import java.io.Serializable
import java.util.Date

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
    ]
)
data class LibraryList(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo("listId")
    val id: Int = 0,
    val ownerId: Int,
    val name: String,
    val description: String? = null,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date(),
) : Serializable
