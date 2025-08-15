package com.flixclusive.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.flixclusive.core.database.entity.User
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM user WHERE userId = :id")
    suspend fun getUserById(id: Int): User?

    @Query("SELECT * FROM user WHERE userId = :id")
    fun getUserByIdInFlow(id: Int): Flow<User?>

    @Query("SELECT * FROM user")
    suspend fun getAllItems(): List<User>

    @Query("SELECT * FROM user")
    fun getAllItemsInFlow(): Flow<List<User>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(item: User): Long

    @Update
    suspend fun update(item: User)

    @Query("DELETE FROM user WHERE userId = :id")
    suspend fun deleteById(id: Int)
}
