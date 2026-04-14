package com.flixclusive.data.database.repository

import com.flixclusive.core.database.entity.user.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun observeUsers(): Flow<List<User>>

    suspend fun getAll(): List<User>

    fun observeUser(id: String): Flow<User?>

    suspend fun addUser(user: User): Long

    suspend fun updateUser(user: User)

    suspend fun deleteUser(id: String)

    suspend fun getUser(id: String): User?
}
