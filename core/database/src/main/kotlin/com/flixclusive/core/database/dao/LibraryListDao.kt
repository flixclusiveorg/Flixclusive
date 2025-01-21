package com.flixclusive.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.flixclusive.model.database.LibraryList
import com.flixclusive.model.database.LibraryListWithItems
import kotlinx.coroutines.flow.Flow

@Dao
interface LibraryListDao {
    @Query("SELECT * FROM library_lists WHERE ownerId = :userId")
    fun getLists(userId: Int): Flow<List<LibraryList>>

    @Query("SELECT * FROM library_lists WHERE listId = :listId")
    fun getList(listId: Int): Flow<LibraryList?>

    @Transaction
    @Query("SELECT * FROM library_lists WHERE listId = :listId")
    fun getListWithItems(listId: Int): Flow<LibraryListWithItems?>

    @Insert
    suspend fun insertList(list: LibraryList): Long

    @Update
    suspend fun updateList(list: LibraryList)

    @Delete
    suspend fun deleteList(list: LibraryList)

    @Query("DELETE FROM library_lists WHERE listId = :listId")
    suspend fun deleteListById(listId: Int)
}
