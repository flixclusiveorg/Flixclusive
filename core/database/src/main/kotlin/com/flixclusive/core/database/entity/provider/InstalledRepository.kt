package com.flixclusive.core.database.entity.provider

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.flixclusive.core.database.entity.user.User
import java.io.Serializable
import java.util.Date

@Entity(
    tableName = "repositories",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["userId"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class InstalledRepository(
    @PrimaryKey
    val url: String,
    val userId: Int,
    val owner: String,
    val name: String,
    val rawLinkFormat: String,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date(),
) : Serializable
