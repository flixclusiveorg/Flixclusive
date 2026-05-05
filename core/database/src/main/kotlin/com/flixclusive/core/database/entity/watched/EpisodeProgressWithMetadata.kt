package com.flixclusive.core.database.entity.watched

import androidx.room.Embedded
import androidx.room.Relation
import com.flixclusive.core.database.entity.media.DBMedia
import com.flixclusive.core.database.entity.media.DBMediaExternalId

/**
 * Represents a progress item in the watch history for an episode,
 *
 * @see WatchProgressWithMetadata
 * @see EpisodeProgress
 * */
data class EpisodeProgressWithMetadata(
    @Embedded override val watchData: EpisodeProgress,
    @Relation(
        parentColumn = "mediaId",
        entityColumn = "id",
    )
    override val media: DBMedia,
    @Relation(
        entity = DBMediaExternalId::class,
        parentColumn = "mediaId",
        entityColumn = "mediaId",
    )
    override val externalIds: List<DBMediaExternalId>,
) : WatchProgressWithMetadata
