package com.flixclusive.feature.splashScreen

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.common.provider.ProviderWithThrowable
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.datastore.model.system.SystemPreferences
import com.flixclusive.core.datastore.model.user.ProviderPreferences
import com.flixclusive.core.datastore.model.user.UserPreferences
import com.flixclusive.data.app.updates.model.AppUpdateInfo
import com.flixclusive.data.app.updates.repository.AppUpdatesRepository
import com.flixclusive.data.database.repository.UserRepository
import com.flixclusive.data.database.session.UserSessionManager
import com.flixclusive.domain.provider.usecase.manage.InitializeProvidersUseCase
import com.flixclusive.domain.provider.usecase.manage.LoadProviderResult
import com.flixclusive.domain.provider.usecase.updater.UpdateProviderUseCase
import com.flixclusive.model.provider.ProviderMetadata
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class SplashScreenViewModel
    @Inject
    constructor(
        userSessionManager: UserSessionManager,
        userRepository: UserRepository,
        private val appUpdatesRepository: AppUpdatesRepository,
        private val dataStoreManager: DataStoreManager,
        private val appDispatchers: AppDispatchers,
        private val initializeProviders: InitializeProvidersUseCase,
        private val updateProviders: UpdateProviderUseCase
    ) : ViewModel() {
        private var saveSettingsJob: Job? = null

        private val _uiState = MutableStateFlow<SplashScreenUiState>(SplashScreenUiState(isLoading = true))
        val uiState = _uiState.asStateFlow()

        val systemPreferences = dataStoreManager
            .getSystemPrefs()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = SystemPreferences(),
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

        fun updateSettings(transform: suspend (t: SystemPreferences) -> SystemPreferences) {
            if (saveSettingsJob?.isActive == true) return

            saveSettingsJob = appDispatchers.ioScope.launch {
                dataStoreManager.updateSystemPrefs(transform)
            }
        }

        fun onConsumeProviderErrors() {
            _uiState.update { it.copy(providerErrors = emptyMap()) }
        }

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
                    _uiState.update { it.copy(appUpdateError = error) }
                }
        }

        /**
         * Initializes the providers when the user logs in for the first time.
         *
         * This is to ensure that the providers are loaded and ready to use.
         * */
        private suspend fun loadProviders() {
            val providers = mutableListOf<ProviderMetadata>()

            initializeProviders()
                .onEach { result ->
                    when (result) {
                        is LoadProviderResult.Success -> {
                            providers += result.provider
                        }

                        is LoadProviderResult.Failure -> {
                            // still collect the provider so it can be passed to updateProvider
                            providers += result.provider

                            _uiState.update { state ->
                                val pair = result.provider.id to ProviderWithThrowable(
                                    provider = result.provider,
                                    throwable = result.error,
                                )

                                state.copy(providerErrors = state.providerErrors + pair)
                            }
                        }
                    }
                }.onCompletion {
                    val providerPrefs =
                        dataStoreManager.getUserPrefs(
                            key = UserPreferences.PROVIDER_PREFS_KEY,
                            type = ProviderPreferences::class
                        ).first()

                    if (providerPrefs.isAutoUpdateEnabled) {
                        updateProviders(providers)
                    }
                }.collect()
        }

        init {
            appDispatchers.ioScope.launch {
                checkForUpdates()

                if (userLoggedIn.value != null) {
                    loadProviders()
                }

                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

@Immutable
internal data class SplashScreenUiState(
    val isLoading: Boolean = false,
    val appUpdateError: Throwable? = null,
    val newAppUpdateInfo: AppUpdateInfo? = null,
    val isInitializingProviders: Boolean = false,
    val providerErrors: Map<String, ProviderWithThrowable> = emptyMap(),
)
