package com.flixclusive.model.database

import androidx.room.Embedded
import androidx.room.Relation

data class UserWithWatchHistoryList(
    @Embedded val user: User,
    @Relation(
        parentColumn = "userId",
        entityColumn = "ownerId"
    )
    val watchHistory: List<WatchHistoryItem>
)