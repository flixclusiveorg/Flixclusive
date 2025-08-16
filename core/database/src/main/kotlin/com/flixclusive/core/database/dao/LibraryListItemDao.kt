package com.flixclusive.core.database.dao

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
    @Query("SELECT * FROM library_list_item_with_metadata WHERE itemId = :itemId")
    suspend fun get(itemId: Long): LibraryListItemWithMetadata?

    @Transaction
    @Query("SELECT * FROM library_list_item_with_metadata WHERE itemId = :itemId")
    fun getAsFlow(itemId: Long): Flow<LibraryListItemWithMetadata?>

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

    @Query("DELETE FROM library_list_items WHERE itemId = :itemId")
    suspend fun delete(itemId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFilm(film: DBFilm)

    @Insert
    suspend fun insertItem(list: LibraryListItem): Long
}
