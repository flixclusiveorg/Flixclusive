package com.flixclusive.core.database.dao.library

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.flixclusive.core.database.entity.film.DBFilm
import com.flixclusive.core.database.entity.library.LibraryListItem
import com.flixclusive.core.database.entity.library.LibraryListItemWithMetadata
import kotlinx.coroutines.flow.Flow

@Dao
interface LibraryListItemDao {
    @Transaction
    @Query("SELECT * FROM library_list_item_with_metadata WHERE id = :id")
    suspend fun get(id: Long): LibraryListItemWithMetadata?

    @Transaction
    @Query("SELECT * FROM library_list_item_with_metadata WHERE id = :id")
    fun getAsFlow(id: Long): Flow<LibraryListItemWithMetadata?>

    @Transaction
    @Query("SELECT * FROM library_list_item_with_metadata WHERE listId = :listId ORDER BY createdAt DESC")
    fun getByListId(listId: Int): Flow<List<LibraryListItemWithMetadata>>

    @Transaction
    suspend fun insert(
        item: LibraryListItem,
        film: DBFilm? = null,
    ): Long {
        if (film != null) {
            insertFilm(film)
        }

        return insertItem(item)
    }

    @Query("DELETE FROM library_list_items WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM library_list_items WHERE listId = :listId AND filmId = :filmId")
    suspend fun deleteByListIdAndFilmId(listId: Int, filmId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFilm(film: DBFilm)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(list: LibraryListItem): Long
}
