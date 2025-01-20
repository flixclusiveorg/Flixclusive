package com.flixclusive.model.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

@Entity(
    tableName = "library_list_entries",
    foreignKeys = [
        ForeignKey(
            entity = LibraryList::class,
            parentColumns = ["listId"],
            childColumns = ["listId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["listId"]),
        Index(value = ["itemId", "listId"], unique = true)
    ]
)
data class LibraryListItem(
    @PrimaryKey(autoGenerate = true) val entryId: Long = 0,
    val listId: String,
    @ColumnInfo(name = "itemId") val libraryItemId: LibraryItemId,
    val addedAt: Date = Date()
)

data class LibraryItemId(
    val providerId: String,
    val itemId: String
) {
    override fun toString(): String = "$providerId:$itemId"

    companion object {
        fun fromString(value: String): LibraryItemId {
            val (providerId, itemId) = value.split(":")
            return LibraryItemId(providerId, itemId)
        }
    }
}
