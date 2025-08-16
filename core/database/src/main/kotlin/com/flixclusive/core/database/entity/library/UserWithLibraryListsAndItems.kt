package com.flixclusive.core.database.entity.library

import androidx.room.Embedded
import androidx.room.Relation
import com.flixclusive.core.database.entity.user.User

data class UserWithLibraryListsAndItems(
    @Embedded val user: User,
    @Relation(
        entity = LibraryList::class,
        parentColumn = "userId",
        entityColumn = "ownerId",
    )
    val lists: List<LibraryListWithItems>,
)
