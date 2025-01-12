package com.flixclusive.feature.mobile.profiles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.util.coroutines.AppDispatchers.Companion.launchOnIO
import com.flixclusive.core.util.coroutines.AppDispatchers.Companion.runOnIO
import com.flixclusive.data.user.UserRepository
import com.flixclusive.domain.user.UserSessionManager
import com.flixclusive.model.database.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
internal class UserProfilesViewModel
    @Inject
    constructor(
        private val userSessionManager: UserSessionManager,
        userRepository: UserRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(ProfilesScreenUiState())
        val uiState = _uiState.asStateFlow()

        val profiles =
            userRepository
                .observeUsers()
                .stateIn(
                    scope = viewModelScope,
                    started = WhileSubscribed(5000),
                    initialValue =
                        runOnIO {
                            userRepository.observeUsers().first()
                        },
                )

        fun onUseProfile(user: User) {
            launchOnIO {
                signOutOldSession()
                userSessionManager.signIn(user)
                _uiState.update { it.copy(isLoggingIn = true) }
            }
        }

        private suspend fun signOutOldSession() {
            userSessionManager.signOut()
        }
    }

internal data class ProfilesScreenUiState(
    val isLoggingIn: Boolean = false,
)
