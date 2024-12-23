package com.flixclusive.feature.mobile.profiles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.data.user.UserRepository
import com.flixclusive.domain.user.UserSessionManager
import com.flixclusive.model.database.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
internal class UserProfilesViewModel @Inject constructor(
    private val userSessionManager: UserSessionManager,
    userRepository: UserRepository,
) : ViewModel() {
    val profiles = userRepository.observeUsers()
        .stateIn(
            scope = viewModelScope,
            started = WhileSubscribed(5000),
            initialValue = emptyList()
        )

    suspend fun onUseProfile(user: User) {
        userSessionManager.signIn(user)
    }
}
