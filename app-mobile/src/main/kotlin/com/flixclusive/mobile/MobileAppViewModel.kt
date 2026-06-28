package com.flixclusive.mobile

import androidx.compose.runtime.Stable
import androidx.compose.ui.util.fastFilteredMap
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.common.provider.ProviderWithThrowable
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.core.datastore.model.user.ProviderPreferences
import com.flixclusive.core.datastore.model.user.UserPreferences
import com.flixclusive.core.network.monitor.NetworkMonitor
import com.flixclusive.core.presentation.player.PlayerCache
import com.flixclusive.core.util.log.infoLog
import com.flixclusive.core.util.webview.WebViewDriverManager
import com.flixclusive.domain.provider.usecase.manage.InitializeProvidersUseCase
import com.flixclusive.domain.provider.usecase.manage.ProviderResult
import com.flixclusive.domain.provider.usecase.updater.CheckOutdatedProviderResult
import com.flixclusive.domain.provider.usecase.updater.CheckOutdatedProviderUseCase
import com.flixclusive.domain.provider.usecase.updater.UpdateProviderUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

internal sealed class ProviderUpdateInfo {
    data class Updated(
        val providerNames: List<String>
    ) : ProviderUpdateInfo()

    data class Outdated(
        val providerNames: List<String>
    ) : ProviderUpdateInfo()
}

@HiltViewModel
internal class MobileAppViewModel @Inject constructor(
    private val dataStoreManager: DataStoreManager,
    private val userSessionDataStore: UserSessionDataStore,
    private val appDispatchers: AppDispatchers,
    private val playerCache: PlayerCache,
    private val initializeProviders: InitializeProvidersUseCase,
    private val checkOutdatedProviders: CheckOutdatedProviderUseCase,
    private val updateProvider: UpdateProviderUseCase,
    networkMonitor: NetworkMonitor,
) : ViewModel() {
    private val _uiState = MutableStateFlow(MobileAppUiState())
    val uiState: StateFlow<MobileAppUiState> = _uiState.asStateFlow()

    private val _providerUpdateInfo = MutableSharedFlow<ProviderUpdateInfo?>()
    val providerUpdateInfo = _providerUpdateInfo.asSharedFlow()

    /**
     * A WebView driver instance that is shared across the app.
     *
     * This is initialized by providers that require a WebView to fetch media links.
     * It is destroyed when the user leaves the app or when it's no longer needed to free up resources.
     * */
    val webViewDriver = WebViewDriverManager.webView
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null,
        )

    /**
     * A StateFlow to check if user is connected to the internet.
     * */
    val hasInternet = networkMonitor.isOnline
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = true,
        )

//    val hasNotSeenNewChangelogs = dataStoreManager
//        .getSystemPrefs()
//        .mapLatest { BuildConfig.VERSION_CODE > it.lastSeenChangelogs }
//        .distinctUntilChanged()
//        .stateIn(
//            scope = viewModelScope,
//            started = SharingStarted.Eagerly,
//            initialValue = true,
//        )

    init {
        viewModelScope.launch {
            val userId = userSessionDataStore.currentUserId.filterNotNull().first()

            launch {
                initializeProviders.isLoading.collect { isLoading ->
                    _uiState.update { it.copy(isLoadingProviders = isLoading) }
                }
            }

            // Ensure that the onboarding process has been completed before loading providers for the first time
            dataStoreManager.getSystemPrefs().first { prefs -> !prefs.isFirstTimeUserLaunch }

            infoLog("Loading $userId's providers for the first time...")
            initProviders()
            updateProviders()
        }
    }

    private suspend fun initProviders() {
        initializeProviders()
            .onEach { result ->
                if (result !is ProviderResult.Failure) return@onEach

                _uiState.update { state ->
                    val pair = result.provider.id to ProviderWithThrowable(
                        provider = result.provider,
                        throwable = result.error,
                    )

                    state.copy(providerErrors = state.providerErrors + pair)
                }
            }.collect()
    }

    private suspend fun updateProviders() {
        val providerPrefs = dataStoreManager
            .getUserPrefsAsFlow(
                key = UserPreferences.PROVIDER_PREFS_KEY,
                type = ProviderPreferences::class
            ).first()

        val outdatedProviders = checkOutdatedProviders()
            .fastFilteredMap(
                predicate = { it is CheckOutdatedProviderResult.Outdated },
                transform = { it.metadata }
            )

        if (outdatedProviders.isEmpty()) return
        if (!providerPrefs.isAutoUpdateEnabled) {
            val names = outdatedProviders.fastMap { it.name }
            _providerUpdateInfo.emit(ProviderUpdateInfo.Outdated(names))
            return
        }

        val results = updateProvider(outdatedProviders)

        // Remove providers that were updated successfully from the errors list in the ui state
        results.success.forEach {
            _uiState.update { state ->
                state.copy(providerErrors = state.providerErrors - it.id)
            }
        }

        // Add providers that failed to update to the errors list in the ui state
        results.failed.forEach { (provider, throwable) ->
            val pair = provider.id to ProviderWithThrowable(
                provider = provider,
                throwable = throwable ?: Error("Failed to update provider"),
            )

            _uiState.update { state ->
                state.copy(providerErrors = state.providerErrors + pair)
            }
        }

        if (results.success.isNotEmpty()) {
            val names = results.success.fastMap { it.name }
            _providerUpdateInfo.emit(ProviderUpdateInfo.Updated(names))
        }
    }

    fun onConsumeProviderErrors() {
        _uiState.update { state ->
            state.copy(providerErrors = emptyMap())
        }
    }

    fun hideWebViewDriver() {
        WebViewDriverManager.destroy()
    }

    fun onSaveLastSeenChangelogs(version: Long) {
        appDispatchers.ioScope.launch {
            dataStoreManager.updateSystemPrefs {
                it.copy(lastSeenChangelogs = version)
            }
        }
    }

    fun onReleasePlayerCache() {
        appDispatchers.ioScope.launch {
            playerCache.release()
        }
    }
}

@Stable
internal data class MobileAppUiState(
    val isLoadingProviders: Boolean = false,
    val providerErrors: Map<String, ProviderWithThrowable> = emptyMap(),
)
