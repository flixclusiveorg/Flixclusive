package com.flixclusive.core.database.entity.watched

import com.flixclusive.core.database.entity.media.DBMedia

/**
 * Represents a watch progress item with associated metadata.
 * */
sealed interface WatchProgressWithMetadata {
    val watchData: WatchProgress
    val media: DBMedia

    val id get() = watchData.id
    val mediaId get() = media.id
    val createdAt get() = watchData.createdAt
    val updatedAt get() = watchData.updatedAt
}
