package com.flixclusive.feature.mobile.profiles

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.compose.ui.util.fastFilter
import androidx.core.app.NotificationCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.database.entity.user.User
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.datastore.model.user.ProviderPreferences
import com.flixclusive.core.datastore.model.user.UserPreferences
import com.flixclusive.core.util.android.notify
import com.flixclusive.data.database.repository.UserRepository
import com.flixclusive.data.database.session.UserSessionManager
import com.flixclusive.data.provider.repository.ProviderApiRepository
import com.flixclusive.data.provider.repository.ProviderRepository
import com.flixclusive.domain.provider.usecase.manage.InitializeProvidersUseCase
import com.flixclusive.domain.provider.usecase.manage.LoadProviderResult
import com.flixclusive.domain.provider.usecase.updater.ProviderUpdateResult
import com.flixclusive.domain.provider.usecase.updater.UpdateProviderUseCase
import com.flixclusive.model.provider.ProviderMetadata
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.flixclusive.core.drawables.R as UiCommonR
import com.flixclusive.core.strings.R as LocaleR

@HiltViewModel
internal class UserProfilesViewModel
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val userSessionManager: UserSessionManager,
        private val initializeProviders: InitializeProvidersUseCase,
        private val updateProvider: UpdateProviderUseCase,
        private val providerRepository: ProviderRepository,
        private val providerApiRepository: ProviderApiRepository,
        private val appDispatchers: AppDispatchers,
        private val dataStoreManager: DataStoreManager,
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
            if (loginJob?.isActive == true) return

            loginJob = appDispatchers.ioScope.launch {
                _uiState.update { it.copy(isLoading = true, errors = emptyMap()) }

                userSessionManager.signOut()
                providerRepository.clearAll()
                providerApiRepository.clearAll()
                userSessionManager.signIn(user)

                loadProviders()
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
                                val pair = result.provider.id to result

                                state.copy(errors = state.errors + pair)
                            }
                        }
                    }
                }.onCompletion {
                    updateProviders(providers)

                    _uiState.update {
                        it.copy(
                            isLoggedIn = true,
                            isLoading = false,
                        )
                    }
                }.collect()
        }

        /**
         * Updates the providers if the auto-update is enabled in the settings.
         *
         * This is called after the providers are initialized in [loadProviders].
         * */
        private suspend fun updateProviders(providers: List<ProviderMetadata>) {
            val providerPrefs = dataStoreManager
                .getUserPrefs(
                    UserPreferences.PROVIDER_PREFS_KEY,
                    ProviderPreferences::class,
                ).first()

            val isAutoUpdateEnabled = providerPrefs.isAutoUpdateEnabled

            if (isAutoUpdateEnabled && providers.isNotEmpty()) {
                val results = updateProvider(providers)

                // Remove providers that were updated successfully from the errors list in the ui state
                results.success.forEach {
                    _uiState.update { state ->
                        state.copy(errors = state.errors - it.id)
                    }
                }

                // Add providers that failed to update to the errors list in the ui state
                results.failed.forEach { (provider, throwable) ->
                    val pair = provider.id to LoadProviderResult.Failure(
                        provider = provider,
                        filePath = "",
                        error = throwable ?: Error("Failed to update provider"),
                    )

                    _uiState.update { state ->
                        state.copy(errors = state.errors + pair)
                    }
                }

                if (results.success.isNotEmpty()) {
                    notifyUpdates(results)
                }
            }
        }

        /**
         * Creates a notification to inform the user about the updated providers.
         *
         * This is only called when the auto-update is enabled in the settings.
         * */
        private fun notifyUpdates(results: ProviderUpdateResult) {
            val updatedProviders = results.success.joinToString(", ") { it.name }
            val message = "The following providers have been updated: $updatedProviders"

            context.notify(
                id = (System.currentTimeMillis() / 1000).toInt(),
                channelId = UpdateProviderUseCase.NOTIFICATION_ID,
                channelName = UpdateProviderUseCase.NOTIFICATION_NAME,
                shouldInitializeChannel = true,
            ) {
                setContentTitle(context.getString(LocaleR.string.flixclusive_providers))
                setContentText(message)
                setSmallIcon(UiCommonR.drawable.provider_logo)
                setOnlyAlertOnce(false)
                setAutoCancel(true)
                setColorized(true)
                setSilent(true)
                setStyle(
                    NotificationCompat
                        .BigTextStyle()
                        .bigText(message),
                )
            }
        }

        private fun List<User>.filterOutCurrentLoggedInUser() =
            fastFilter { it.id != userSessionManager.currentUser.value?.id }

        fun onHoverProfile(user: User) {
            _uiState.update { it.copy(focusedProfile = user) }
        }

        fun onConsumeErrors() {
            _uiState.update { it.copy(errors = emptyMap()) }
        }
    }

@Immutable
internal data class ProfilesScreenUiState(
    val isLoggedIn: Boolean = false,
    val isLoading: Boolean = false,
    val focusedProfile: User? = null,
    val errors: Map<String, LoadProviderResult.Failure> = emptyMap(),
)
