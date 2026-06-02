package com.flixclusive.core.database.entity.provider

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.flixclusive.core.database.entity.media.DBMedia
import java.util.Date
import java.util.UUID

/**
 * Generates a time-ordered UUID (v7-compatible layout) without external dependencies.
 *
 * Layout:
 * - bits  0-47  : Unix timestamp in milliseconds
 * - bits 48-51  : version field (0111 = 7)
 * - bits 52-63  : random
 * - bits 64-65  : variant field (10)
 * - bits 66-127 : random
 */
private fun generateUUIDv7(): String {
    val ts = System.currentTimeMillis()
    val rand = UUID.randomUUID()
    val msb = (ts shl 16) or 0x7000L or (rand.mostSignificantBits and 0x0FFFL)
    val lsb = (rand.leastSignificantBits and 0x3FFFFFFFFFFFFFFFL) or Long.MIN_VALUE
    return UUID(msb, lsb).toString()
}

@Entity(
    tableName = "cached_media_links",
    foreignKeys = [
        ForeignKey(
            entity = InstalledProvider::class,
            parentColumns = ["id", "ownerId"],
            childColumns = ["providerId", "ownerId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = DBMedia::class,
            parentColumns = ["id"],
            childColumns = ["mediaId"],
            onDelete = ForeignKey.NO_ACTION,
        ),
    ],
    indices = [
        Index(value = ["ownerId"]),
        Index(value = ["mediaId"]),
        Index(value = ["providerId", "ownerId"]),
        Index(
            value = ["ownerId", "mediaId", "episodeNumber", "seasonNumber"],
            unique = true,
        ),
    ],
)
data class CachedMediaLinks(
    @PrimaryKey val id: String = generateUUIDv7(),
    val providerId: String,
    val ownerId: String,
    val mediaId: String,
    val episodeNumber: Int? = null,
    val seasonNumber: Int? = null,
    val thumbnail: String? = null,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date(),
)
