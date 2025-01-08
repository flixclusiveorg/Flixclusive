package com.flixclusive.feature.mobile.settings.screen.root

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.datastore.util.asStateFlow
import com.flixclusive.core.util.coroutines.AppDispatchers
import com.flixclusive.data.provider.ProviderManager
import com.flixclusive.data.search_history.SearchHistoryRepository
import com.flixclusive.data.user.UserRepository
import com.flixclusive.domain.provider.GetMediaLinksUseCase
import com.flixclusive.domain.user.UserSessionManager
import com.flixclusive.model.datastore.system.SystemPreferences
import com.flixclusive.model.datastore.user.ProviderPreferences
import com.flixclusive.model.datastore.user.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class SettingsViewModel
    @Inject
    constructor(
        val userSessionManager: UserSessionManager,
        private val userRepository: UserRepository,
        private val dataStoreManager: DataStoreManager,
        private val searchHistoryRepository: SearchHistoryRepository,
        private val getMediaLinksUseCase: GetMediaLinksUseCase,
        private val providerManager: ProviderManager,
    ) : ViewModel() {
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

        val cachedLinksSize by derivedStateOf {
            getMediaLinksUseCase.cache.size
        }

        val systemPreferences =
            dataStoreManager.systemPreferences
                .asStateFlow(viewModelScope)

        inline fun <reified T : UserPreferences> getUserPrefsAsState(key: Preferences.Key<String>) =
            dataStoreManager
                .getUserPrefs<T>(key)
                .asStateFlow(viewModelScope)

        suspend fun updateSystemPrefs(transform: suspend (t: SystemPreferences) -> SystemPreferences): Boolean {
            dataStoreManager.updateSystemPrefs(transform)
            return true
        }

        suspend inline fun <reified T : UserPreferences> updateUserPrefs(
            key: Preferences.Key<String>,
            crossinline transform: suspend (t: T) -> T,
        ): Boolean {
            dataStoreManager.updateUserPrefs<T>(key, transform)
            return true
        }

        fun clearSearchHistory() {
            AppDispatchers.IO.scope.launch {
                val userId =
                    userSessionManager.currentUser.value?.id
                        ?: return@launch

                searchHistoryRepository.clearAll(userId)
            }
        }

        fun clearCacheLinks() {
            getMediaLinksUseCase.cache.clear()
        }

        fun deleteRepositories() {
            AppDispatchers.IO.scope.launch {
                updateUserPrefs<ProviderPreferences>(UserPreferences.PROVIDER_PREFS_KEY) {
                    it.copy(repositories = emptyList())
                }
            }
        }

        fun deleteProviders() {
            AppDispatchers.IO.scope.launch {
                with(providerManager) {
                    workingProviders.first().forEach {
                        unload(it)
                    }
                }
            }
        }
    }
