package com.flixclusive.core.database.entity.provider

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable
import java.util.Date

@Entity(tableName = "repositories")
data class InstalledRepository(
    @PrimaryKey
    val url: String,
    val owner: String,
    val name: String,
    val rawLinkFormat: String,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date(),
) : Serializable
