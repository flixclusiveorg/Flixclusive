package com.flixclusive.feature.mobile.profiles

import androidx.compose.runtime.Immutable
import androidx.compose.ui.util.fastFilter
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.database.entity.user.User
import com.flixclusive.data.database.repository.UserRepository
import com.flixclusive.data.database.session.UserSessionManager
import com.flixclusive.data.provider.repository.ProviderApiRepository
import com.flixclusive.data.provider.repository.ProviderRepository
import com.flixclusive.domain.provider.usecase.manage.InitializeProvidersUseCase
import com.flixclusive.domain.provider.usecase.manage.LoadProviderResult
import com.flixclusive.domain.provider.usecase.updater.UpdateProviderUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class UserProfilesViewModel
    @Inject
    constructor(
        private val userSessionManager: UserSessionManager,
        private val initializeProviders: InitializeProvidersUseCase,
        private val updateProvider: UpdateProviderUseCase,
        private val providerRepository: ProviderRepository,
        private val providerApiRepository: ProviderApiRepository,
        private val appDispatchers: AppDispatchers,
        userRepository: UserRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(ProfilesScreenUiState())
        val uiState = _uiState.asStateFlow()

        private var loginJob: Job? = null

        val profiles = userRepository
            .observeUsers()
            .mapLatest { it.filterOutCurrentLoggedInUser() }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = emptyList(),
            )

        fun onUseProfile(user: User) {
            if  (loginJob?.isActive == true) return

            loginJob = appDispatchers.ioScope.launch {
                _uiState.update { it.copy(isLoading = true, errors = emptyList()) }

                userSessionManager.signOut()
                providerRepository.clearAll()
                providerApiRepository.clearAll()
                userSessionManager.signIn(user)

                initializeProviders()
                    .onEach { status ->
                        when (status) {
                            is LoadProviderResult.Failure -> {
                                _uiState.update { state ->
                                    state.copy(errors = state.errors + status)
                                }
                            }
                            is LoadProviderResult.Success -> TODO()
                        }
                    }
                    .onCompletion {
                        _uiState.update { it.copy(isLoggedIn = true) }
                    }
            }
        }

        private fun List<User>.filterOutCurrentLoggedInUser() =
            fastFilter { it.id != userSessionManager.currentUser.value?.id }

        fun onHoverProfile(user: User) {
            _uiState.update { it.copy(focusedProfile = user) }
        }
    }

@Immutable
internal data class ProfilesScreenUiState(
    val isLoggedIn: Boolean = false,
    val isLoading: Boolean = false,
    val focusedProfile: User? = null,
    val errors: List<LoadProviderResult.Failure> = emptyList()
)
