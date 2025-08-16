package com.flixclusive.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.flixclusive.core.database.entity.library.LibraryList
import com.flixclusive.core.database.entity.library.LibraryListWithItems
import com.flixclusive.core.database.entity.library.UserWithLibraryListsAndItems
import kotlinx.coroutines.flow.Flow

@Dao
interface LibraryListDao {
    @Query("SELECT * FROM library_lists WHERE ownerId = :userId")
    fun getAll(userId: Int): Flow<List<LibraryList>>

    @Query("SELECT * FROM library_lists WHERE listId = :listId")
    fun get(listId: Int): Flow<LibraryList?>

    // TODO: Support sorting
    @Transaction
    @Query("""
        SELECT DISTINCT list.*
        FROM library_lists list
        INNER JOIN library_list_items listItem ON list.listId = listItem.listId
        WHERE listItem.filmId = :filmId AND list.ownerId = :ownerId
        ORDER BY list.createdAt DESC
    """)
    fun getListsContainingFilm(filmId: String, ownerId: Int): Flow<List<LibraryList>>

    @Transaction
    @Query("SELECT * FROM library_lists WHERE listId = :listId")
    fun getListWithItems(listId: Int): Flow<LibraryListWithItems?>

    @Transaction
    @Query("SELECT * FROM User WHERE userId = :userId")
    fun getUserWithListsAndItems(userId: Int): Flow<UserWithLibraryListsAndItems>

    @Insert
    suspend fun insert(list: LibraryList): Long

    @Update
    suspend fun update(list: LibraryList)

    @Query("DELETE FROM library_lists WHERE listId = :listId")
    suspend fun delete(listId: Int)
}
