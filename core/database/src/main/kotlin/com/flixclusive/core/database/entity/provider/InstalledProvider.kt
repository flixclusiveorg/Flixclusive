package com.flixclusive.core.database.entity.provider

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.flixclusive.core.database.entity.user.User
import java.io.File
import java.io.Serializable
import java.util.Date

@Entity(
    tableName = "installed_providers",
    primaryKeys = ["id", "ownerId"],
    foreignKeys = [
        ForeignKey(
            entity = InstalledRepository::class,
            parentColumns = ["url", "userId"],
            childColumns = ["repositoryUrl", "ownerId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = User::class,
            parentColumns = ["userId"],
            childColumns = ["ownerId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["repositoryUrl"]),
        Index(value = ["ownerId"]),
        Index(value = ["repositoryUrl", "ownerId"]),
        Index(value = ["sortOrder"]),
        Index(value = ["isEnabled"]),
    ],
)
data class InstalledProvider(
    val id: String,
    val ownerId: String,
    val repositoryUrl: String,
    val filePath: String,
    val sortOrder: Double,
    val isEnabled: Boolean = true,
    val isDebug: Boolean = false,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date(),
) : Serializable {
    val file get() = File(filePath)
}
