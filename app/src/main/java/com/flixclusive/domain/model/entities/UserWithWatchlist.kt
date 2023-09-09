package com.flixclusive.domain.model.entities

import androidx.room.Embedded
import androidx.room.Relation

data class UserWithWatchlist(
    @Embedded val user: User,
    @Relation(
        parentColumn = "userId",
        entityColumn = "ownerId"
    )
    val watchlist: List<WatchlistItem>
)