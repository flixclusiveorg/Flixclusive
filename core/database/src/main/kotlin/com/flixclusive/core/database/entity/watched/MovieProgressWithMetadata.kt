package com.flixclusive.core.database.entity.watched

import androidx.room.Embedded
import androidx.room.Relation
import com.flixclusive.core.database.entity.media.DBMedia

/**
 * Represents a movie watch progress with associated library list item metadata.
 *
 * @see MovieProgress
 * @see WatchProgressWithMetadata
 * */
data class MovieProgressWithMetadata(
    @Embedded override val watchData: MovieProgress,
    @Relation(
        parentColumn = "mediaId",
        entityColumn = "id",
    )
    override val media: DBMedia,
) : WatchProgressWithMetadata
