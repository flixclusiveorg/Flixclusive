package com.flixclusive.core.database.entity.provider

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.flixclusive.core.database.entity.user.User
import java.io.File
import java.io.Serializable
import java.util.Date

@Entity(
    tableName = "installed_providers",
    foreignKeys = [
        ForeignKey(
            entity = InstalledRepository::class,
            parentColumns = ["url"],
            childColumns = ["repositoryUrl"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["ownerId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["repositoryUrl"]),
        Index(value = ["ownerId"]),
        Index(value = ["sortOrder"]),
        Index(value = ["isEnabled"]),
    ],
)
data class InstalledProvider(
    @PrimaryKey val id: String,
    val ownerId: Int,
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
