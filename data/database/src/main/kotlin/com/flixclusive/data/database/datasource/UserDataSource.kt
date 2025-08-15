package com.flixclusive.data.database.datasource

import com.flixclusive.core.database.entity.User
import kotlinx.coroutines.flow.Flow

interface UserDataSource {
    fun observeUsers(): Flow<List<User>>

    fun observeUser(id: Int): Flow<User?>

    suspend fun addUser(user: User): Long

    suspend fun updateUser(user: User)

    suspend fun deleteUser(id: Int)

    suspend fun getUser(id: Int): User?
}
