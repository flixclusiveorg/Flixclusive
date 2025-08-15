package com.flixclusive.data.database.repository.impl

import com.flixclusive.core.database.entity.User
import com.flixclusive.data.database.datasource.UserDataSource
import com.flixclusive.data.database.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

internal class UserRepositoryImpl
    @Inject
    constructor(
        private val dataSource: UserDataSource,
    ) : UserRepository {
        override fun observeUsers(): Flow<List<User>> = dataSource.observeUsers()

        override suspend fun getUser(id: Int) = dataSource.getUser(id)

        override fun observeUser(id: Int): Flow<User?> = dataSource.observeUser(id)

        override suspend fun addUser(user: User): Long = dataSource.addUser(user)

        override suspend fun updateUser(user: User) = dataSource.updateUser(user)

        override suspend fun deleteUser(id: Int) = dataSource.deleteUser(id)
    }
