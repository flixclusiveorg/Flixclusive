package com.flixclusive.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.flixclusive.core.database.entity.user.User
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM user WHERE userId = :id")
    suspend fun get(id: Int): User?

    @Query("SELECT * FROM user WHERE userId = :id")
    fun getAsFlow(id: Int): Flow<User?>

    @Query("SELECT * FROM user")
    suspend fun getAll(): List<User>

    @Query("SELECT * FROM user")
    fun getAllAsFlow(): Flow<List<User>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(item: User): Long

    @Update
    suspend fun update(item: User)

    @Query("DELETE FROM user WHERE userId = :id")
    suspend fun delete(id: Int)
}
