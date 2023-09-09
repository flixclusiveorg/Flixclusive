package com.flixclusive.data.repository

import com.flixclusive.data.database.dao.UserDao
import com.flixclusive.di.IoDispatcher
import com.flixclusive.domain.model.entities.User
import com.flixclusive.domain.repository.UserRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val userDao: UserDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
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