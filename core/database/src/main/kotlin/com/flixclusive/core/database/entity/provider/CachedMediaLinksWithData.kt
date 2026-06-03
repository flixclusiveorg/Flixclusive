package com.flixclusive.core.database.entity.provider

import androidx.room.Embedded
import androidx.room.Relation
import com.flixclusive.core.database.entity.media.DBMedia

data class CachedMediaLinksWithData(
    @Embedded val cache: CachedMediaLinks,
    @Relation(
        parentColumn = "id",
        entityColumn = "parentId",
        entity = DBStream::class,
    )
    val streams: List<DBStream>,
    @Relation(
        parentColumn = "id",
        entityColumn = "parentId",
        entity = DBSubtitle::class,
    )
    val subtitles: List<DBSubtitle>,
    @Relation(
        parentColumn = "mediaId",
        entityColumn = "id",
        entity = DBMedia::class,
    )
    val media: DBMedia,
) {
    val id: String get() = cache.id
    val size: Int get() = streams.size
    val providerId: String get() = cache.providerId
    val hasValidLinks: Boolean get() = streams.any { it.isAliveAndValid }
}
