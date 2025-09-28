package com.flixclusive.feature.mobile.settings.screen.root

import androidx.compose.runtime.Stable
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.common.config.BuildConfigProvider
import com.flixclusive.core.common.config.CustomBuildConfig
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.datastore.model.system.SystemPreferences
import com.flixclusive.core.datastore.model.user.ProviderPreferences
import com.flixclusive.core.datastore.model.user.UserPreferences
import com.flixclusive.data.database.repository.SearchHistoryRepository
import com.flixclusive.data.database.session.UserSessionManager
import com.flixclusive.data.provider.repository.CachedLinksRepository
import com.flixclusive.data.provider.repository.ProviderRepository
import com.flixclusive.domain.provider.usecase.manage.UnloadProviderUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class SettingsViewModel
    @Inject
    constructor(
        private val userSessionManager: UserSessionManager,
        private val dataStoreManager: DataStoreManager,
        private val searchHistoryRepository: SearchHistoryRepository,
        private val providerRepository: ProviderRepository,
        private val unloadProviderUseCase: UnloadProviderUseCase,
        private val cachedLinksRepository: CachedLinksRepository,
        private val appDispatchers: AppDispatchers,
        private val _buildConfig: BuildConfigProvider,
    ) : ViewModel() {
        val currentUser = userSessionManager.currentUser

        /**
         * This contains the [CustomBuildConfig] provided by the DI.
         *
         * It includes:
         * - versionName
         * - versionCode
         * - commitHash
         * - buildType
         * - applicationId
         * - applicationName
         * */
        @Stable
        val buildConfig get() = _buildConfig.get()

        val searchHistoryCount =
            userSessionManager.currentUser
                .filterNotNull()
                .flatMapLatest { user ->
                    searchHistoryRepository
                        .getAllItemsInFlow(ownerId = user.id)
                        .map { it.size }
                }.stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = 0,
                )

        val cachedLinksSize =
            cachedLinksRepository.caches
                .mapLatest { it.size }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = 0,
                )

        val systemPreferences = dataStoreManager
            .getSystemPrefs()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
            initialValue = SystemPreferences(),
        )

        inline fun <reified T : UserPreferences> getUserPrefsAsState(key: Preferences.Key<String>) =
            dataStoreManager
                .getUserPrefs<T>(key, type = T::class)
                .distinctUntilChanged()
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = T::class.java.getDeclaredConstructor().newInstance(),
                )

        suspend fun updateSystemPrefs(transform: suspend (t: SystemPreferences) -> SystemPreferences): Boolean {
            dataStoreManager.updateSystemPrefs(transform)
            return true
        }

        suspend inline fun <reified T : UserPreferences> updateUserPrefs(
            key: Preferences.Key<String>,
            noinline transform: suspend (t: T) -> T,
        ): Boolean {
            dataStoreManager.updateUserPrefs<T>(key, type = T::class, transform = transform)
            return true
        }

        fun clearSearchHistory() {
            appDispatchers.ioScope.launch {
                val userId = userSessionManager.currentUser
                    .filterNotNull()
                    .first()
                    .id

                searchHistoryRepository.clearAll(userId)
            }
        }

        fun clearCacheLinks() {
            cachedLinksRepository.clear()
        }

        fun deleteRepositories() {
            appDispatchers.ioScope.launch {
                updateUserPrefs<ProviderPreferences>(UserPreferences.PROVIDER_PREFS_KEY) {
                    it.copy(repositories = emptyList())
                }
            }
        }

        fun deleteProviders() {
            appDispatchers.ioScope.launch {
                providerRepository.getProviders().forEach {
                    unloadProviderUseCase(it)
                }
            }
        }
    }
