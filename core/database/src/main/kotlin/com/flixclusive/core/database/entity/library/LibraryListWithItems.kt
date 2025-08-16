package com.flixclusive.core.database.entity.library

import androidx.room.Embedded
import androidx.room.Relation

data class LibraryListWithItems(
    @Embedded val list: LibraryList,
    @Relation(
        parentColumn = "listId",
        entityColumn = "listId",
    )
    val items: List<LibraryListItemWithMetadata>,
)
