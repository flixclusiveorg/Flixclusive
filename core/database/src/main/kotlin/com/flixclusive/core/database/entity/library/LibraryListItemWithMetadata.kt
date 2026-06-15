package com.flixclusive.core.database.entity.library

import androidx.room.DatabaseView
import androidx.room.Embedded
import androidx.room.Ignore
import androidx.room.Relation
import com.flixclusive.core.database.entity.media.DBMedia
import com.flixclusive.core.database.entity.media.DBMedia.Companion.toMediaMetadata
import com.flixclusive.core.database.entity.media.DBMediaExternalId
import com.flixclusive.core.database.entity.media.DBMediaExternalId.Companion.toExternalIdMap
import com.flixclusive.model.media.MediaMetadata

/**
 * Convenience view for library list items with their associated media metadata.
 * */
@Suppress("ktlint:standard:max-line-length")
@DatabaseView(
    viewName = "library_list_item_with_metadata",
    value = "SELECT library_list_items.id AS item_id, library_list_items.mediaId AS item_mediaId, library_list_items.listId AS item_listId, library_list_items.createdAt AS item_createdAt, library_list_items.updatedAt AS item_updatedAt, media.id AS media_id, media.title AS media_title, media.providerId AS media_providerId, media.type AS media_type, media.overview AS media_overview, media.posterImage AS media_posterImage, media.adult AS media_adult, media.language AS media_language, media.rating AS media_rating, media.backdropImage AS media_backdropImage, media.releaseDate AS media_releaseDate, media.createdAt AS media_createdAt, media.updatedAt AS media_updatedAt FROM library_list_items INNER JOIN media ON library_list_items.mediaId = media.id"
)
data class LibraryListItemWithMetadata(
    @Embedded(prefix = "item_")
    val item: LibraryListItem,
    @Embedded(prefix = "media_")
    val metadata: DBMedia,
    @Relation(
        entity = DBMediaExternalId::class,
        parentColumn = "media_id",
        entityColumn = "mediaId",
    )
    val externalIds: List<DBMediaExternalId>,
) {
    fun toMediaMetadata(): MediaMetadata = metadata.toMediaMetadata(externalIds.toExternalIdMap())

    @get:Ignore
    val itemId: String get() = item.id

    @get:Ignore
    val mediaId: String get() = item.mediaId

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LibraryListItemWithMetadata) return false

        if (itemId != other.itemId) return false
        if (mediaId != other.mediaId) return false

        return true
    }

    override fun hashCode(): Int {
        var result = item.hashCode()
        result = 31 * result + metadata.hashCode()
        result = 31 * result + externalIds.hashCode()
        result = 31 * result + itemId.hashCode()
        result = 31 * result + mediaId.hashCode()
        return result
    }
}
