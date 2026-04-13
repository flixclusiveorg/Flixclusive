package com.flixclusive.feature.splashScreen

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.common.exception.ExceptionWithUiText
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.datastore.model.system.SystemPreferences
import com.flixclusive.data.app.updates.model.AppUpdateInfo
import com.flixclusive.data.app.updates.repository.AppUpdatesRepository
import com.flixclusive.data.database.repository.UserRepository
import com.flixclusive.data.database.session.UserSessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class SplashScreenViewModel @Inject constructor(
    userSessionManager: UserSessionManager,
    userRepository: UserRepository,
    private val appUpdatesRepository: AppUpdatesRepository,
    private val dataStoreManager: DataStoreManager,
    private val appDispatchers: AppDispatchers,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SplashScreenUiState(isLoading = true))
    val uiState = _uiState.asStateFlow()

    val systemPreferences = dataStoreManager
        .getSystemPrefs()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null,
        )

    val noUsersFound = userRepository
        .observeUsers()
        .map { it.isEmpty() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = false,
        )

    val userLoggedIn = userSessionManager.currentUser

    fun onConsumeAppUpdateError() {
        _uiState.update { it.copy(appUpdateError = null) }
    }

    private suspend fun checkForUpdates() {
        _uiState.update { SplashScreenUiState(isLoading = true) }

        appUpdatesRepository
            .getLatestUpdate()
            .onSuccess { appUpdateInfo ->
                _uiState.update {
                    it.copy(
                        newAppUpdateInfo = appUpdateInfo,
                        appUpdateError = null,
                    )
                }
            }.onFailure { error ->
                _uiState.update { it.copy(appUpdateError = error as ExceptionWithUiText) }
            }
    }

    init {
        appDispatchers.ioScope.launch {
            checkForUpdates()

            _uiState.update { it.copy(isLoading = false) }
        }
    }
}

@Immutable
internal data class SplashScreenUiState(
    val isLoading: Boolean = false,
    val appUpdateError: ExceptionWithUiText? = null,
    val newAppUpdateInfo: AppUpdateInfo? = null,
)
