package com.flixclusive.model.database

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation
import java.util.Date

@Entity(tableName = "library_lists")
data class LibraryList(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo("listId")
    val id: Int,
    val ownerId: Int,
    val name: String,
    val description: String? = null,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date(),
)

@Entity(tableName = "library_list_items")
data class LibraryListItem(
    @PrimaryKey
    @ColumnInfo("itemId")
    override val id: String,
    override val film: DBFilm,
) : DBFilmItem

@Entity(
    tableName = "library_list_and_item_cross_ref",
    primaryKeys = ["listId", "itemId"]
)
data class LibraryListAndItemCrossRef(
    val listId: Int,
    val itemId: String,
    val addedOn: Date = Date(),
)

data class LibraryListWithItems(
    @Embedded val list: LibraryList,
    @Relation(
        parentColumn = "listId",
        entityColumn = "itemId",
        associateBy = Junction(LibraryListAndItemCrossRef::class),
    )
    val items: List<LibraryListItem>,
)

data class LibraryListItemWithLists(
    @Embedded val item: LibraryListItem,
    @Relation(
        parentColumn = "itemId",
        entityColumn = "listId",
        associateBy = Junction(LibraryListAndItemCrossRef::class),
    )
    val lists: List<LibraryList>,
)

data class UserWithLibraryListsAndItems(
    @Embedded val user: User,
    @Relation(
        entity = LibraryList::class,
        parentColumn = "userId",
        entityColumn = "ownerId",
    )
    val list: List<LibraryListWithItems>,
)
