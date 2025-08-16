package com.flixclusive.feature.mobile.profiles

import androidx.compose.ui.util.fastFilter
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.database.entity.user.User
import com.flixclusive.core.util.coroutines.AppDispatchers.Companion.launchOnIO
import com.flixclusive.core.util.coroutines.AppDispatchers.Companion.runOnIO
import com.flixclusive.domain.database.repository.UserRepository
import com.flixclusive.domain.home.HomeItemsProviderUseCase
import com.flixclusive.domain.provider.ProviderLoaderUseCase
import com.flixclusive.domain.provider.ProviderUpdaterUseCase
import com.flixclusive.domain.provider.repository.ProviderApiRepository
import com.flixclusive.domain.provider.repository.ProviderRepository
import com.flixclusive.domain.session.UserSessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class UserProfilesViewModel
    @Inject
    constructor(
        private val userSessionManager: UserSessionManager,
        private val providerLoaderUseCase: ProviderLoaderUseCase,
        private val providerUpdaterUseCase: ProviderUpdaterUseCase,
        private val providerRepository: ProviderRepository,
        private val providerApiRepository: ProviderApiRepository,
        private val homeItemsProviderUseCase: HomeItemsProviderUseCase,
        userRepository: UserRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(ProfilesScreenUiState())
        val uiState = _uiState.asStateFlow()

        val profiles =
            userRepository
                .observeUsers()
                .mapLatest { it.filterOutCurrentLoggedInUser() }
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
                userSessionManager.signOut()
                providerRepository.clearAll()
                providerApiRepository.clearAll()
                userSessionManager.signIn(user)
                homeItemsProviderUseCase(user.id)
                providerLoaderUseCase.initDebugFolderToPreferences()
                providerLoaderUseCase.initFromLocal()
                providerUpdaterUseCase(notify = true)
                _uiState.update { it.copy(isLoggingIn = true) }
            }
        }

        private fun List<User>.filterOutCurrentLoggedInUser() =
            fastFilter { it.id != userSessionManager.currentUser.value?.id }
    }

data class ProfilesScreenUiState(
    val isLoggingIn: Boolean = false,
)
