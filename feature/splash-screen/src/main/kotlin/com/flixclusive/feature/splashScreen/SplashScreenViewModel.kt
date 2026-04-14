package com.flixclusive.feature.splashScreen

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.common.exception.ExceptionWithUiText
import com.flixclusive.core.database.entity.user.User
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.datastore.model.system.SystemPreferences
import com.flixclusive.data.app.updates.model.AppUpdateInfo
import com.flixclusive.data.app.updates.repository.AppUpdatesRepository
import com.flixclusive.data.database.repository.UserRepository
import com.flixclusive.data.database.session.UserSessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

internal sealed interface SplashNavigationEvent {
    data object Onboarding : SplashNavigationEvent

    data class AppUpdate(
        val info: AppUpdateInfo,
    ) : SplashNavigationEvent

    data object AddProfile : SplashNavigationEvent
    data object ChooseProfile : SplashNavigationEvent
    data object Home : SplashNavigationEvent
}

@HiltViewModel
internal class SplashScreenViewModel @Inject constructor(
    dataStoreManager: DataStoreManager,
    appDispatchers: AppDispatchers,
    private val userSessionManager: UserSessionManager,
    private val userRepository: UserRepository,
    private val appUpdatesRepository: AppUpdatesRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SplashScreenUiState())
    val uiState = _uiState.asStateFlow()

    private val _navigationEvents = MutableSharedFlow<SplashNavigationEvent>()
    val navigationEvents = _navigationEvents.asSharedFlow()

    val systemPreferences = dataStoreManager
        .getSystemPrefs()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null,
        )

    fun onConsumeAppUpdateError() {
        _uiState.update { it.copy(appUpdateError = null) }
    }

    private suspend fun checkForUpdates() {
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
        viewModelScope.launch {
            checkForUpdates()
            startNavigation()
        }
    }

    private fun startNavigation() {
        viewModelScope.launch {
            val startTime = System.currentTimeMillis()

            val snapshot = combine(
                systemPreferences.filterNotNull(),
                uiState,
                userSessionManager.currentUser,
                userRepository.observeUsers().map { it.isEmpty() }.distinctUntilChanged(),
            ) { preferences, state, currentUser, hasNoUsers ->
                NavigationSnapshot(
                    preferences = preferences,
                    uiState = state,
                    currentUser = currentUser,
                    noUsersFound = hasNoUsers,
                )
            }.first { state -> shouldNavigate(state) }

            val event = buildNavigationEvent(snapshot)
            delayForMinimumDuration(startTime)
            _navigationEvents.emit(event)
        }
    }

    private fun shouldNavigate(snapshot: NavigationSnapshot): Boolean {
        val hasAppUpdateErrors =
            snapshot.uiState.appUpdateError != null &&
                snapshot.preferences.isUsingAutoUpdateAppFeature

        return !hasAppUpdateErrors
    }

    private fun buildNavigationEvent(snapshot: NavigationSnapshot): SplashNavigationEvent {
        val hasAutoUpdate = snapshot.preferences.isUsingAutoUpdateAppFeature
        val updateInfo = snapshot.uiState.newAppUpdateInfo

        return when {
            updateInfo != null && hasAutoUpdate -> SplashNavigationEvent.AppUpdate(updateInfo)
            snapshot.preferences.isFirstTimeUserLaunch -> SplashNavigationEvent.Onboarding
            snapshot.noUsersFound -> SplashNavigationEvent.AddProfile
            snapshot.currentUser == null -> SplashNavigationEvent.ChooseProfile
            else -> SplashNavigationEvent.Home
        }
    }

    private suspend fun delayForMinimumDuration(startTime: Long) {
        val elapsedMillis = System.currentTimeMillis() - startTime
        val remainingMillis = MIN_SPLASH_DURATION_MS - elapsedMillis
        if (remainingMillis > 0L) {
            delay(remainingMillis)
        }
    }
}

private data class NavigationSnapshot(
    val preferences: SystemPreferences,
    val uiState: SplashScreenUiState,
    val currentUser: User?,
    val noUsersFound: Boolean,
)

private const val MIN_SPLASH_DURATION_MS = 3_000L

@Immutable
internal data class SplashScreenUiState(
    val appUpdateError: ExceptionWithUiText? = null,
    val newAppUpdateInfo: AppUpdateInfo? = null,
)
