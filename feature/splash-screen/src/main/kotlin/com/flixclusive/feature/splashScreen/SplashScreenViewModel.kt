package com.flixclusive.feature.splashScreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.datastore.util.asStateFlow
import com.flixclusive.core.network.util.Resource
import com.flixclusive.core.util.coroutines.AppDispatchers.Companion.launchOnIO
import com.flixclusive.data.configuration.AppConfigurationManager
import com.flixclusive.data.provider.ProviderManager
import com.flixclusive.data.user.UserRepository
import com.flixclusive.domain.home.HomeItemsProviderUseCase
import com.flixclusive.domain.home.PREFERRED_MINIMUM_HOME_ITEMS
import com.flixclusive.domain.updater.AppUpdateCheckerUseCase
import com.flixclusive.domain.updater.ProviderUpdaterUseCase
import com.flixclusive.domain.user.UserSessionManager
import com.flixclusive.model.datastore.system.SystemPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

internal sealed class SplashScreenUiState {
    data object Loading : SplashScreenUiState()

    data object Okay : SplashScreenUiState()

    data object Failure : SplashScreenUiState()
}

@HiltViewModel
internal class SplashScreenViewModel
    @Inject
    constructor(
        homeItemsProviderUseCase: HomeItemsProviderUseCase,
        appConfigurationManager: AppConfigurationManager,
        val appUpdateCheckerUseCase: AppUpdateCheckerUseCase,
        private val userSessionManager: UserSessionManager,
        private val userRepository: UserRepository,
        private val dataStoreManager: DataStoreManager,
        private val providerManager: ProviderManager,
        private val providerUpdaterUseCase: ProviderUpdaterUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<SplashScreenUiState>(SplashScreenUiState.Loading)
        val uiState = _uiState.asStateFlow()

        val configurationStatus = appConfigurationManager.configurationStatus

        val systemPreferences =
            dataStoreManager.systemPreferences
                .asStateFlow(viewModelScope)

        val noUsersFound =
            userRepository
                .observeUsers()
                .map { it.isEmpty() }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = runBlocking { userRepository.observeUsers().first().isEmpty() },
                )

        val userLoggedIn =
            userSessionManager
                .currentUser
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = null,
                )

        init {
            viewModelScope.launch {
                launch initHomeScreen@{
                    combine(
                        appConfigurationManager.configurationStatus,
                        userLoggedIn,
                    ) { status, user ->
                        if (status is Resource.Success && user != null) {
                            homeItemsProviderUseCase(user.id)
                            this@initHomeScreen.cancel()
                        }
                    }.collect()
                }

                launch waitForHomeScreenItems@{
                    homeItemsProviderUseCase.state.collectLatest { state ->
                        val newState =
                            when {
                                state.rowItems.size >= PREFERRED_MINIMUM_HOME_ITEMS -> SplashScreenUiState.Okay
                                state.status is Resource.Failure -> SplashScreenUiState.Failure
                                else -> _uiState.value
                            }

                        _uiState.value = newState

                        if (newState == SplashScreenUiState.Okay) {
                            this@waitForHomeScreenItems.cancel()
                        }
                    }
                }
            }

            launchOnIO {
                userLoggedIn.collectLatest {
                    if (it != null) {
                        providerManager.initialize()
                        providerUpdaterUseCase(notify = true)
                        cancel()
                    } else {
                        userSessionManager.restoreSession()
                    }
                }
            }
        }

        fun updateSettings(transform: suspend (t: SystemPreferences) -> SystemPreferences) {
            launchOnIO {
                dataStoreManager.updateSystemPrefs(transform)
            }
        }
    }
