package com.flixclusive.core.database.model

import androidx.room.Embedded
import androidx.room.Relation
import com.flixclusive.model.database.User
import com.flixclusive.model.database.WatchHistoryItem

data class UserWithWatchHistoryList(
    @Embedded val user: User,
    @Relation(
        parentColumn = "userId",
        entityColumn = "ownerId"
    )
    val watchHistory: List<WatchHistoryItem>
)