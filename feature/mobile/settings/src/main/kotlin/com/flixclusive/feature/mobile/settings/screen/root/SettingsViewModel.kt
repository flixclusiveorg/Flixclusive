package com.flixclusive.feature.mobile.settings.screen.root

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Stable
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.common.config.BuildConfigProvider
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.datastore.model.system.SystemPreferences
import com.flixclusive.core.datastore.model.user.BackupOptions
import com.flixclusive.core.datastore.model.user.UserPreferences
import com.flixclusive.data.backup.util.BackupUtil.decodeFromUri
import com.flixclusive.data.database.repository.SearchHistoryRepository
import com.flixclusive.data.database.session.UserSessionManager
import com.flixclusive.data.provider.repository.CachedLinksRepository
import com.flixclusive.data.provider.repository.InstalledRepoRepository
import com.flixclusive.data.provider.repository.ProviderRepository
import com.flixclusive.domain.backup.common.BackupState
import com.flixclusive.domain.backup.usecase.CreateBackupUseCase
import com.flixclusive.domain.backup.usecase.RestoreBackupUseCase
import com.flixclusive.domain.provider.usecase.manage.InitializeProvidersUseCase
import com.flixclusive.domain.provider.usecase.manage.UnloadProviderUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collect
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
internal class SettingsViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val userSessionManager: UserSessionManager,
    private val dataStoreManager: DataStoreManager,
    private val searchHistoryRepository: SearchHistoryRepository,
    private val providerRepository: ProviderRepository,
    private val unloadProviderUseCase: UnloadProviderUseCase,
    private val cachedLinksRepository: CachedLinksRepository,
    private val appDispatchers: AppDispatchers,
    private val installedRepoRepository: InstalledRepoRepository,
    private val initializeProviders: InitializeProvidersUseCase,
    private val _buildConfig: BuildConfigProvider,
    private val createBackupUseCase: CreateBackupUseCase,
    private val restoreBackupUseCase: RestoreBackupUseCase,
) : ViewModel() {
    val currentUser = userSessionManager.currentUser

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

    val providers = userSessionManager.currentUser
        .filterNotNull()
        .flatMapLatest { user ->
            providerRepository.getInstalledProvidersAsFlow(user.id)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    val repositories = userSessionManager.currentUser
        .filterNotNull()
        .flatMapLatest { user ->
            installedRepoRepository.getAllAsFlow(user.id)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    inline fun <reified T : UserPreferences> getUserPrefsAsState(key: Preferences.Key<String>) =
        dataStoreManager
            .getUserPrefs(key, type = T::class)
            .distinctUntilChanged()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = T::class.java.getDeclaredConstructor().newInstance(),
            )

    fun updateSystemPrefs(transform: suspend (t: SystemPreferences) -> SystemPreferences) {
        appDispatchers.ioScope.launch {
            dataStoreManager.updateSystemPrefs(transform)
        }
    }

    inline fun <reified T : UserPreferences> updateUserPrefs(
        key: Preferences.Key<String>,
        noinline transform: suspend (t: T) -> T,
    ) {
        appDispatchers.ioScope.launch {
            dataStoreManager.updateUserPrefs(key, type = T::class, transform = transform)
        }
    }

    fun clearSearchHistory() {
        appDispatchers.ioScope.launch {
            val userId = getCurrentUserId()
            searchHistoryRepository.clearAll(userId)
        }
    }

    fun clearCacheLinks() {
        cachedLinksRepository.clear()
    }

    fun deleteRepositories() {
        appDispatchers.ioScope.launch {
            val userId = getCurrentUserId()
            installedRepoRepository.deleteAll(userId)
        }
    }

    fun deleteProviders() {
        appDispatchers.ioScope.launch {
            val userId = getCurrentUserId()
            providerRepository.getInstalledProviders(userId).forEach {
                unloadProviderUseCase(it)
            }
        }
    }

    suspend fun createBackup(
        uri: Uri,
        options: BackupOptions = BackupOptions(),
    ): BackupState {
        return createBackupUseCase(
            uri = uri,
            options = options,
        ).first { state -> state is BackupState.Success || state is BackupState.Error }
    }

    suspend fun restoreBackup(
        uri: Uri,
        options: BackupOptions = BackupOptions(),
    ): BackupState {
        val backup = context.decodeFromUri(uri = uri)

        val includeProviders = options.includeProviders && backup.providers.isNotEmpty()
        if (includeProviders) {
            val userId = getCurrentUserId()
            providerRepository.getInstalledProviders(userId).forEach {
                unloadProviderUseCase(it, uninstall = false)
            }
        }

        return restoreBackupUseCase(
            uri = uri,
            options = options,
        ).first { state -> state is BackupState.Success || state is BackupState.Error }
            .also {
                if (includeProviders) {
                    initializeProviders().collect()
                }
            }
    }

    private suspend fun getCurrentUserId(): String {
        return userSessionManager.currentUser
            .filterNotNull()
            .map { it.id }
            .first()
    }
}
