package com.flixclusive.data.user

import com.flixclusive.data.user.local.UserDataSource
import com.flixclusive.model.database.User
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

internal class DefaultUserRepository @Inject constructor(
    private val dataSource: UserDataSource
) : UserRepository {
    override fun observeUsers(): Flow<List<User>>
        = dataSource.observeUsers()

    override suspend fun getUser(id: Int)
        = dataSource.getUser(id)

    override fun observeUser(id: Int): Flow<User?>
        = dataSource.observeUser(id)

    override suspend fun addUser(user: User)
        = dataSource.addUser(user)

    override suspend fun updateUser(user: User)
        = dataSource.updateUser(user)

    override suspend fun deleteUser(id: Int)
        = dataSource.deleteUser(id)
}