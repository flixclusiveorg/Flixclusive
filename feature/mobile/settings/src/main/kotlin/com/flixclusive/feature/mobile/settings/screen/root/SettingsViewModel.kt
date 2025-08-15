package com.flixclusive.feature.mobile.settings.screen.root

import androidx.compose.runtime.Immutable
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.datastore.util.asStateFlow
import com.flixclusive.core.util.coroutines.AppDispatchers.Companion.launchOnIO
import com.flixclusive.core.util.coroutines.asStateFlow
import com.flixclusive.data.configuration.AppBuild
import com.flixclusive.data.configuration.AppConfigurationManager
import com.flixclusive.data.provider.cache.CachedLinksRepository
import com.flixclusive.domain.database.repository.SearchHistoryRepository
import com.flixclusive.domain.provider.ProviderUnloaderUseCase
import com.flixclusive.domain.provider.repository.ProviderRepository
import com.flixclusive.domain.session.UserSessionManager
import com.flixclusive.model.datastore.system.SystemPreferences
import com.flixclusive.model.datastore.user.ProviderPreferences
import com.flixclusive.model.datastore.user.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltViewModel
internal class SettingsViewModel
    @Inject
    constructor(
        val userSessionManager: UserSessionManager,
        private val appConfigurationManager: AppConfigurationManager,
        private val dataStoreManager: DataStoreManager,
        private val searchHistoryRepository: SearchHistoryRepository,
        private val providerRepository: ProviderRepository,
        private val providerUnloaderUseCase: ProviderUnloaderUseCase,
        private val cachedLinksRepository: CachedLinksRepository,
    ) : ViewModel() {
        private val isUsingPrereleaseUpdates = dataStoreManager.systemPreferences.data
            .map { it.isUsingPrereleaseUpdates }
            .distinctUntilChanged()

        val appBuildWithPrereleaseFlag = isUsingPrereleaseUpdates
            .map { isUsingPrereleaseUpdates ->
                AppBuildWithPrereleaseFlag(
                    appBuild = appConfigurationManager.currentAppBuild!!, // Ensure this is not null!
                    isPrerelease = isUsingPrereleaseUpdates,
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = AppBuildWithPrereleaseFlag(
                    appBuild = appConfigurationManager.currentAppBuild!!,
                    isPrerelease = runBlocking { isUsingPrereleaseUpdates.first() },
                )
            )

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
                .asStateFlow(viewModelScope)

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
            launchOnIO {
                val userId =
                    userSessionManager.currentUser.value?.id
                        ?: return@launchOnIO

                searchHistoryRepository.clearAll(userId)
            }
        }

        fun clearCacheLinks() {
            cachedLinksRepository.clear()
        }

        fun deleteRepositories() {
            launchOnIO {
                updateUserPrefs<ProviderPreferences>(UserPreferences.PROVIDER_PREFS_KEY) {
                    it.copy(repositories = emptyList())
                }
            }
        }

        fun deleteProviders() {
            launchOnIO {
                providerRepository.getProviders().forEach {
                    providerUnloaderUseCase.unload(it)
                }
            }
        }
    }

@Immutable
internal data class AppBuildWithPrereleaseFlag(
    private val appBuild: AppBuild,
    val isPrerelease: Boolean,
) {
    val versionName: String get() = appBuild.versionName
    val commitVersion: String get() = appBuild.commitVersion
    val isDebug: Boolean get() = appBuild.debug
}
