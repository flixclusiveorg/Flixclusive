package com.flixclusive.feature.mobile.user.add

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.database.entity.user.User
import com.flixclusive.data.database.repository.LibraryListRepository
import com.flixclusive.data.database.repository.UserAuthRepository
import com.flixclusive.data.database.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

internal sealed class AddUserState {
    data object Added : AddUserState()

    data object NotAdded : AddUserState()
}

@HiltViewModel
class AddUserViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val userAuthRepository: UserAuthRepository,
    private val libraryListRepository: LibraryListRepository,
    private val appDispatchers: AppDispatchers,
) : ViewModel() {
    val images by lazy {
        listOf(
            "https://image.tmdb.org/t/p/original/13bHg4hwhPqauZhxgMzCLSIAM89.jpg",
            "https://image.tmdb.org/t/p/original/JfEDrH4QObfVvFtnqzZfkUp9x4.jpg",
            "https://image.tmdb.org/t/p/original/6WmpbBcZeo6Vr9f7dMqpBobCSjR.jpg",
            "https://image.tmdb.org/t/p/original/cjrSkULmG2btwLOEvWZCeO5KRY2.jpg",
            "https://image.tmdb.org/t/p/original/bMSbEx9vXCSGN4NEktjVIEuibn2.jpg",
            "https://image.tmdb.org/t/p/original/5UhrZoYLLlbigxS578hyQn2qf9W.jpg",
        ).shuffled()
    }

    private val _state = MutableStateFlow<AddUserState>(AddUserState.NotAdded)
    internal val state = _state.asStateFlow()

    val user = mutableStateOf(User.Empty)

    private var addJob: Job? = null

    fun addUser(
        user: User,
        isSigningIn: Boolean,
    ) {
        if (addJob?.isActive == true) {
            return
        }

        addJob = appDispatchers.ioScope.launch {
            val userId = user.id.ifBlank { UUID.randomUUID().toString() }
            val validatedUser = user.copy(id = userId)

            userRepository.addUser(validatedUser)
            libraryListRepository.seedLists(userId)

            if (isSigningIn) {
                userAuthRepository.signIn(validatedUser)
            }

            _state.value = AddUserState.Added
        }
    }
}
