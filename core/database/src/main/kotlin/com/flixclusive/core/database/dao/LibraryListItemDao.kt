package com.flixclusive.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.flixclusive.model.database.LibraryListItem
import com.flixclusive.model.database.LibraryListItemWithLists
import kotlinx.coroutines.flow.Flow

@Dao
interface LibraryListItemDao {
    @Query("SELECT * FROM library_list_items WHERE itemId = :itemId")
    fun getItem(itemId: String): Flow<LibraryListItem?>

    @Transaction
    @Query("SELECT * FROM library_list_items WHERE itemId = :itemId")
    fun getItemWithLists(itemId: String): Flow<LibraryListItemWithLists?>

    @Insert
    suspend fun insertItem(item: LibraryListItem): Long

    @Update
    suspend fun updateItem(item: LibraryListItem)

    @Delete
    suspend fun deleteItem(item: LibraryListItem)
}
