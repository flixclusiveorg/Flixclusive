package com.flixclusive.core.database.entity.media

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.flixclusive.model.media.MediaMetadata
import com.flixclusive.model.media.common.MediaIdSource
import java.util.Date

@Entity(
    tableName = "media_external_ids",
    primaryKeys = ["mediaId", "providerId", "source"],
    foreignKeys = [
        ForeignKey(
            entity = DBMedia::class,
            parentColumns = ["id"],
            childColumns = ["mediaId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["mediaId"]),
        Index(value = ["providerId"]),
    ],
)
data class DBMediaExternalId(
    val mediaId: String,
    val providerId: String,
    val source: String,
    val externalId: String,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date(),
) {
    companion object {
        fun MediaMetadata.toDBMediaExternalIds(): List<DBMediaExternalId> {
            return externalIds.map { (source, externalId) ->
                DBMediaExternalId(
                    mediaId = id,
                    providerId = providerId,
                    source = source.name,
                    externalId = externalId,
                )
            }
        }

        fun List<DBMediaExternalId>.toExternalIdMap(): Map<MediaIdSource, String> {
            return associate { MediaIdSource.valueOf(it.source) to it.externalId }
        }
    }
}
