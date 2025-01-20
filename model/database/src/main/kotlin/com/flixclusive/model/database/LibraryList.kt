package com.flixclusive.model.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "library_lists")
data class LibraryList(
    @PrimaryKey
    @ColumnInfo("listId")
    val id: String,
    val ownerId: Int,
    val name: String,
    val description: String? = null,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
)
