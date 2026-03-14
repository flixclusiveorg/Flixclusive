package com.flixclusive.core.database.entity.provider

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
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
    ],
    indices = [
        Index(value = ["repositoryUrl"]),
        Index(value = ["sortOrder"]),
        Index(value = ["status"]),
        Index(value = ["providerType"]),
    ],
)
data class InstalledProvider(
    @PrimaryKey val id: String,
    val repositoryUrl: String,
    val name: String,
    val status: String,
    val providerType: String,
    val language: String,
    val adult: Boolean,
    val versionName: String,
    val versionCode: Long,
    val buildUrl: String,
    val iconUrl: String? = null,
    val isDisabled: Boolean = false,
    val isDebug: Boolean = false,
    val sortOrder: Double,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date(),
) : Serializable
