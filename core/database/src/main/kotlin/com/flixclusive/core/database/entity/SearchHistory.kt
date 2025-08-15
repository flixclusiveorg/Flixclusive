package com.flixclusive.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

@Entity(
    tableName = "search_history",
    indices = [Index(value = ["query", "ownerId"], unique = true)]
)
data class SearchHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val query: String,
    val ownerId: Int,
    val searchedOn: Date = Date()
)
