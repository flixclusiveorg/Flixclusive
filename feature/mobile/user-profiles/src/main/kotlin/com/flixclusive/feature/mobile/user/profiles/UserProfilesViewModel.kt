package com.flixclusive.feature.mobile.user.profiles

import androidx.compose.runtime.Immutable
import androidx.compose.ui.util.fastFilter
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.database.entity.user.User
import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.data.database.repository.UserAuthRepository
import com.flixclusive.data.database.repository.UserRepository
import com.flixclusive.data.provider.repository.ProviderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@HiltViewModel
internal class UserProfilesViewModel @Inject constructor(
    private val userAuthRepository: UserAuthRepository,
    private val providerRepository: ProviderRepository,
    private val appDispatchers: AppDispatchers,
    userSessionDataStore: UserSessionDataStore,
    userRepository: UserRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(UserProfilesUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<UserProfilesEvent>()
    val events = _events.asSharedFlow()

    private var loginJob: Job? = null

    @OptIn(FlowPreview::class)
    val profiles = combine(
        userRepository.observeUsers(),
        userSessionDataStore.currentUserId.debounce(300.milliseconds),
    ) { users, loggedInUser ->
        users.fastFilter { it.id != loggedInUser }
            .also {
                _uiState.update { it.copy(isLoadingProfiles = false) }
            }
    }.onStart {
        _uiState.update { it.copy(isLoadingProfiles = true) }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = emptyList(),
    )

    fun onUseProfile(user: User) {
        if (loginJob?.isActive == true) return

        loginJob = appDispatchers.ioScope.launch {
            userAuthRepository.signOut()
            providerRepository.clearAll()
            userAuthRepository.signIn(user)

            _events.emit(UserProfilesEvent.Login)
        }
    }
}

internal sealed class UserProfilesEvent {
    data object Login : UserProfilesEvent()
}

@Immutable
internal data class UserProfilesUiState(
    val isLoadingProfiles: Boolean = true
)
