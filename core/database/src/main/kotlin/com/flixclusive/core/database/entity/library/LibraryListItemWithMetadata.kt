package com.flixclusive.core.database.entity.library

import androidx.room.DatabaseView
import androidx.room.Embedded
import androidx.room.Ignore
import androidx.room.Relation
import com.flixclusive.core.database.entity.film.DBFilm
import com.flixclusive.core.database.entity.film.DBFilmExternalId

/**
 * Convenience view for library list items with their associated film metadata.
 * */
@DatabaseView(
    viewName = "library_list_item_with_metadata",
    value = """
        SELECT
            library_list_items.id AS item_id,
            library_list_items.filmId AS item_filmId,
            library_list_items.listId AS item_listId,
            library_list_items.createdAt AS item_createdAt,
            library_list_items.updatedAt AS item_updatedAt,

            films.id AS film_id,
            films.title AS film_title,
            films.providerId AS film_providerId,
            films.filmType AS film_filmType,
            films.overview AS film_overview,
            films.posterImage AS film_posterImage,
            films.adult AS film_adult,
            films.language AS film_language,
            films.rating AS film_rating,
            films.backdropImage AS film_backdropImage,
            films.releaseDate AS film_releaseDate,
            films.year AS film_year,
            films.createdAt AS film_createdAt,
            films.updatedAt AS film_updatedAt
        FROM library_list_items
        INNER JOIN films
        ON library_list_items.filmId = films.id
    """
)
data class LibraryListItemWithMetadata(
    @Embedded(prefix = "item_")
    val item: LibraryListItem,
    @Embedded(prefix = "film_")
    val metadata: DBFilm,
    @Relation(
        entity = DBFilmExternalId::class,
        parentColumn = "film_id",
        entityColumn = "filmId",
    )
    val externalIds: List<DBFilmExternalId>,
) {
    @get:Ignore
    val itemId: Long get() = item.id

    @get:Ignore
    val filmId: String get() = item.filmId
}
