package com.flixclusive.model.database

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
    val ownerId: Int = 1,
    val searchedOn: Date = Date(),
    val query: String
)
