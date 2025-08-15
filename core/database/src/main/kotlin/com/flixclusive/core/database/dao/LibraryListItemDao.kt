package com.flixclusive.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.flixclusive.core.database.entity.LibraryListItem
import kotlinx.coroutines.flow.Flow

@Dao
interface LibraryListItemDao {
    @Query("SELECT * FROM library_list_items WHERE itemId = :itemId")
    fun getItem(itemId: String): Flow<LibraryListItem?>

    @Insert
    suspend fun insertItem(item: LibraryListItem): Long

    @Update
    suspend fun updateItem(item: LibraryListItem)

    @Query("DELETE FROM library_list_items WHERE itemId = :itemId")
    suspend fun deleteItemById(itemId: String)
}
