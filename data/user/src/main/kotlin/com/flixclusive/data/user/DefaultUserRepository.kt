package com.flixclusive.data.user

import com.flixclusive.core.database.dao.UserDao
import com.flixclusive.core.util.common.network.AppDispatchers
import com.flixclusive.core.util.common.network.Dispatcher
import com.flixclusive.model.database.User
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject

internal class DefaultUserRepository @Inject constructor(
    private val userDao: UserDao,
    @Dispatcher(AppDispatchers.IO) private val ioDispatcher: CoroutineDispatcher
) : UserRepository {
    override fun getAllUsers(): Flow<List<User>> = userDao.getAllItemsInFlow()

    override suspend fun getUserById(id: Int): User? {
        return withContext(ioDispatcher) {
            userDao.getUserById(id)
        }
    }

    override fun getUserByIdFlow(id: Int): Flow<User?> = userDao.getUserByIdInFlow(id)

    override suspend fun insert(user: User) {
        withContext(ioDispatcher) {
            userDao.insert(user)
        }
    }

    override suspend fun deleteById(id: Int) {
        withContext(ioDispatcher) {
            userDao.deleteById(id)
        }
    }
}