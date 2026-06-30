package com.flixclusive.feature.splashScreen

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.common.exception.ExceptionWithUiText
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.core.datastore.model.system.SystemPreferences
import com.flixclusive.data.app.updates.model.AppUpdateInfo
import com.flixclusive.data.app.updates.repository.AppUpdatesRepository
import com.flixclusive.data.database.repository.UserRepository
import com.flixclusive.domain.provider.usecase.manage.InitializeProvidersUseCase
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
import kotlin.time.Duration.Companion.milliseconds

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
    private val userSessionDataStore: UserSessionDataStore,
    private val userRepository: UserRepository,
    private val appUpdatesRepository: AppUpdatesRepository,
    private val initializeProviders: InitializeProvidersUseCase,
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
                userSessionDataStore.currentUserId,
                userRepository.observeUsers().map { it.isEmpty() }.distinctUntilChanged(),
                initializeProviders.isLoading,
            ) { preferences, state, currentUserId, hasNoUsers, isLoadingProviders ->
                NavigationSnapshot(
                    preferences = preferences,
                    uiState = state,
                    currentUserId = currentUserId,
                    noUsersFound = hasNoUsers,
                    isLoadingProviders = isLoadingProviders,
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

        return !hasAppUpdateErrors && !snapshot.isLoadingProviders
    }

    private fun buildNavigationEvent(snapshot: NavigationSnapshot): SplashNavigationEvent {
        val hasAutoUpdate = snapshot.preferences.isUsingAutoUpdateAppFeature
        val updateInfo = snapshot.uiState.newAppUpdateInfo

        return when {
            snapshot.preferences.isFirstTimeUserLaunch -> SplashNavigationEvent.Onboarding
            snapshot.currentUserId == null -> SplashNavigationEvent.ChooseProfile
            snapshot.noUsersFound -> SplashNavigationEvent.AddProfile
            updateInfo != null && hasAutoUpdate -> SplashNavigationEvent.AppUpdate(updateInfo)
            else -> SplashNavigationEvent.Home
        }
    }

    private suspend fun delayForMinimumDuration(startTime: Long) {
        val elapsedMillis = System.currentTimeMillis() - startTime
        val remainingMillis = MIN_SPLASH_DURATION_MS - elapsedMillis
        if (remainingMillis > 0L) {
            delay(remainingMillis.milliseconds)
        }
    }
}

private data class NavigationSnapshot(
    val preferences: SystemPreferences,
    val uiState: SplashScreenUiState,
    val currentUserId: String?,
    val noUsersFound: Boolean,
    val isLoadingProviders: Boolean,
)

private const val MIN_SPLASH_DURATION_MS = 3_000L

@Immutable
internal data class SplashScreenUiState(
    val appUpdateError: ExceptionWithUiText? = null,
    val newAppUpdateInfo: AppUpdateInfo? = null,
)
