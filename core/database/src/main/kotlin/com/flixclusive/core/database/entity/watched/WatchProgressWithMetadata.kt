package com.flixclusive.core.database.entity.watched

import com.flixclusive.core.database.entity.media.DBMedia
import com.flixclusive.core.database.entity.media.DBMedia.Companion.toMediaMetadata
import com.flixclusive.core.database.entity.media.DBMediaExternalId
import com.flixclusive.core.database.entity.media.DBMediaExternalId.Companion.toExternalIdMap
import com.flixclusive.model.media.PartialMedia

/**
 * Represents a watch progress item with associated metadata.
 * */
sealed interface WatchProgressWithMetadata {
    val watchData: WatchProgress
    val media: DBMedia
    val externalIds: List<DBMediaExternalId>

    val id get() = watchData.id
    val mediaId get() = media.id
    val createdAt get() = watchData.createdAt
    val updatedAt get() = watchData.updatedAt

    fun toMediaMetadata(): PartialMedia = media.toMediaMetadata(externalIds.toExternalIdMap())
}
