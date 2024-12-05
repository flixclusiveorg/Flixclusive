package com.flixclusive.data.user

import com.flixclusive.model.database.User
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

internal class DefaultUserRepositoryTest {
    private lateinit var fakeDataSource: FakeDataSource
    private lateinit var repository: DefaultUserRepository

    @Before
    fun setup() {
        fakeDataSource = FakeDataSource()
        repository = DefaultUserRepository(fakeDataSource)
    }

    @Test
    fun `test adding and observing users`() = runTest {
        // Arrange
        val testUser = User(name = "Test User")

        // Act
        repository.addUser(testUser)

        // Assert
        val users = repository.observeUsers().first()
        assert(users.contains(element = testUser))
    }

    @Test
    fun `test getting specific user`() = runTest {
        // Arrange
        val testUser = User(id = 1, name = "Test User")
        fakeDataSource.setUsers(listOf(testUser))

        // Act
        val retrievedUser = repository.getUser(1)

        // Assert
        assert(retrievedUser?.equals(testUser) == true)
    }
}
