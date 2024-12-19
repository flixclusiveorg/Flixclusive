package com.flixclusive.feature.mobile.user.edit

import com.flixclusive.data.user.UserRepository
import com.flixclusive.model.database.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Before
import org.junit.Test

private const val MAX_USERS = 2

class UserEditViewModelTest {
    private lateinit var viewModel: UserEditViewModel
    private lateinit var fakeUserRepository: UserRepository
    private lateinit var users: List<User>
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        users = List(MAX_USERS) {
            User(
                id = it + 1,
                name = "User ${it + 1}"
            )
        }
        fakeUserRepository = FakeUserRepository(
            users = users,
            dispatcher = testDispatcher
        )
        viewModel = UserEditViewModel(
            userRepository = fakeUserRepository
        )
    }

    @Test
    fun onRemoveUser() = runTest {
        val user = users.first()
        viewModel.onRemoveUser(user)

        val newList = fakeUserRepository
            .observeUsers()
            .first()

        assert(!newList.contains(user))
    }

    @Test
    fun onEditUser() = runTest {
        val user = users.last()
        val newUsername = "New Name"
        val newUser = user.copy(name = newUsername)

        viewModel.onEditUser(newUser)
        val newList = fakeUserRepository
            .observeUsers()
            .first()

        assert(newList.size == MAX_USERS)
        assert(newList.contains(newUser))
        assert(newList.last().name == newUsername)
    }
}