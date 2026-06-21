package com.flixclusive.core.database.entity.provider

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.flixclusive.core.database.entity.media.DBMedia
import java.util.Date

@Entity(
    tableName = "cached_subtitles",
    foreignKeys = [
        ForeignKey(
            entity = DBMedia::class,
            parentColumns = ["id"],
            childColumns = ["mediaId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["ownerId"]),
        Index(value = ["mediaId"]),
        Index(value = ["providerId"]),
        Index(value = ["mediaId", "seasonNumber", "episodeNumber"]),
    ],
)
data class CachedSubtitle(
    @PrimaryKey override val url: String,
    @ColumnInfo(name = "language") override val label: String,
    override val description: String? = null,
    override val customHeaders: Map<String, String>? = null,
    override val isDead: Boolean = false,
    override val providerId: String,
    override val ownerId: String,
    override val mediaId: String,
    override val episodeNumber: Int? = null,
    override val seasonNumber: Int? = null,
    override val createdAt: Date = Date(),
    override val updatedAt: Date = Date(),
    val subtitleSource: String = "ONLINE",
) : CachedMediaLink
