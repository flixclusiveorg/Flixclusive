package com.flixclusive.data.user

import com.flixclusive.model.database.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FakeUserRepositoryTest {
    private val defaultUsers = listOf(
        User(id = 1, name = "John Doe", image = 1),
        User(id = 2, name = "Emma Smith", image = 2),
        User(id = 3, name = "Michael Johnson", image = 3),
        User(id = 4, name = "Sophia Williams", image = 4),
        User(id = 5, name = "David Brown", image = 5)
    )
    private val repository = FakeUserRepository(
        defaultUsers,
        Dispatchers.Unconfined
    )

    @Test
    fun `insert and get user works`() = runTest {
        // Arrange
        val user = User(id = 6, name = "Justin Dark")

        // Act
        repository.addUser(user)

        // Assert
        assertEquals(user, repository.getUser(user.id))
    }

    @Test
    fun `delete user works`() = runTest {
        // Arrange
        val user = User(id = 6, name = "Justin Dark")
        repository.addUser(user)

        // Act
        repository.deleteUser(user.id)

        // Assert
        assertNull(repository.getUser(user.id))
    }

    @Test
    fun `get all users returns correct list`() = runTest {
        // Arrange
        val user1 = User(id = 1, name = "John Doe", image = 1)
        val user2 = User(id = 2, name = "Emma Smith", image = 2)
        repository.getUser(user1.id)
        repository.getUser(user2.id)

        // Act
        val users = repository.observeUsers().first()

        // Assert
        assertEquals(5, users.size)
        assertEquals(setOf(user1, user2), users.take(2).toSet())
    }

    @Test
    fun `get user by id flow works`() = runTest {
        // Arrange
        val user = User(id = 6, name = "Justin Dark")
        repository.addUser(user)

        // Act
        val userFlow = repository.observeUser(user.id).first()

        // Assert
        assertEquals(user, userFlow)
    }
}