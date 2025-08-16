package com.flixclusive.core.database.entity.library

import androidx.room.DatabaseView
import androidx.room.Embedded
import androidx.room.Ignore
import com.flixclusive.core.database.entity.film.DBFilm

/**
 * Convenience view for library list items with their associated film metadata.
 * */
@DatabaseView(
    viewName = "library_list_item_with_metadata",
    value = "SELECT library_list_items.*, films.* FROM library_list_items " +
        "INNER JOIN films ON library_list_items.filmId = films.id",
)
data class LibraryListItemWithMetadata(
    @Embedded val item: LibraryListItem,
    @Embedded val metadata: DBFilm,
) {
    @get:Ignore
    val itemId: Long get() = item.id

    @get:Ignore
    val filmId: String get() = item.filmId
}
