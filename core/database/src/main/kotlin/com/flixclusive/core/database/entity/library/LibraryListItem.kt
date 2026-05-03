package com.flixclusive.core.database.entity.library

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.flixclusive.core.database.entity.media.DBMedia
import java.util.Date
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Entity(
    tableName = "library_list_items",
    foreignKeys = [
        ForeignKey(
            entity = LibraryList::class,
            parentColumns = ["id"],
            childColumns = ["listId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = DBMedia::class,
            parentColumns = ["id"],
            childColumns = ["mediaId"],
        ),
    ],
    indices = [
        Index(value = ["mediaId", "listId"], unique = true),
        Index(value = ["mediaId"]),
        Index(value = ["listId"]),
    ],
)
data class LibraryListItem(
    @PrimaryKey
    val id: String = Uuid.generateV4().toString(),
    val mediaId: String,
    val listId: String,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date(),
)
