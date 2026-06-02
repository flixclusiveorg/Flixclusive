package com.flixclusive.core.database.entity.provider

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import java.util.Date

@Entity(
    tableName = "cached_subtitles",
    primaryKeys = ["url", "parentId"],
    foreignKeys = [
        ForeignKey(
            entity = CachedMediaLinks::class,
            parentColumns = ["id"],
            childColumns = ["parentId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["parentId"]),
    ],
)
data class DBSubtitle(
    override val url: String,
    override val parentId: String,
    @ColumnInfo(name = "language") override val label: String,
    override val description: String? = null,
    override val customHeaders: Map<String, String>? = null,
    override val isDead: Boolean = false,
    override val createdAt: Date = Date(),
    override val updatedAt: Date = Date(),
    val subtitleSource: String = "ONLINE",
) : DBMediaLink
