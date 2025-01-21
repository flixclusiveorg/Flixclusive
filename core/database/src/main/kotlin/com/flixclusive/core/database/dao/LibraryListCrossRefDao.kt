package com.flixclusive.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.flixclusive.model.database.LibraryListAndItemCrossRef
import com.flixclusive.model.database.UserWithLibraryListsAndItems
import kotlinx.coroutines.flow.Flow

@Dao
interface LibraryListCrossRefDao {
    @Insert
    suspend fun insertCrossRef(crossRef: LibraryListAndItemCrossRef)

    @Delete
    suspend fun deleteCrossRef(crossRef: LibraryListAndItemCrossRef)

    @Query("DELETE FROM library_list_and_item_cross_ref WHERE listId = :listId AND itemId = :itemId")
    suspend fun deleteCrossRefById(listId: Int, itemId: String)

    @Transaction
    @Query("SELECT * FROM User WHERE userId = :userId")
    fun getUserWithListsAndItems(userId: Int): Flow<UserWithLibraryListsAndItems?>

    @Transaction
    @Query("SELECT * FROM library_list_and_item_cross_ref WHERE listId = :listId AND itemId = :itemId")
    fun getItemAddedDetails(listId: Int, itemId: String): Flow<LibraryListAndItemCrossRef?>
}
