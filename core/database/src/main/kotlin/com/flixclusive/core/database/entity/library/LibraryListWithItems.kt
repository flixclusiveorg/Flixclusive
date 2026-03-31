package com.flixclusive.core.database.entity.library

import androidx.room.Embedded
import androidx.room.Relation

data class LibraryListWithItems(
    @Embedded val list: LibraryList,
    @Relation(
        entity = LibraryListItemWithMetadata::class,
        parentColumn = "id",
        entityColumn = "item_listId",
    )
    val items: List<LibraryListItemWithMetadata>,
) {
    val id get() = list.id
    val ownerId get() = list.ownerId
    val listType get() = list.listType
}
