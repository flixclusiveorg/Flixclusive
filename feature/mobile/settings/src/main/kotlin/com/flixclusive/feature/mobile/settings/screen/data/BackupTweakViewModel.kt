package com.flixclusive.feature.mobile.settings.screen.data

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.datastore.DataStoreManager.Companion.getUserPrefs
import com.flixclusive.core.datastore.DataStoreManager.Companion.getUserPrefsAsFlow
import com.flixclusive.core.datastore.DataStoreManager.Companion.updateUserPrefs
import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.core.datastore.model.system.SystemPreferences
import com.flixclusive.core.datastore.model.user.BackupOptions
import com.flixclusive.core.datastore.model.user.DataPreferences
import com.flixclusive.core.datastore.model.user.UserPreferences
import com.flixclusive.core.util.exception.safeCall
import com.flixclusive.data.backup.util.BackupUtil.decodeFromUri
import com.flixclusive.data.database.repository.SearchHistoryRepository
import com.flixclusive.data.provider.repository.MediaLinksRepository
import com.flixclusive.data.provider.repository.ProviderRepository
import com.flixclusive.domain.backup.common.BackupState
import com.flixclusive.domain.backup.usecase.CreateBackupUseCase
import com.flixclusive.domain.backup.usecase.RestoreBackupUseCase
import com.flixclusive.domain.provider.usecase.manage.InitializeProvidersUseCase
import com.flixclusive.domain.provider.usecase.manage.UnloadProviderUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltViewModel
internal class BackupTweakViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val appDispatchers: AppDispatchers,
    private val createBackupUseCase: CreateBackupUseCase,
    private val dataStoreManager: DataStoreManager,
    private val initializeProviders: InitializeProvidersUseCase,
    private val mediaLinksRepository: MediaLinksRepository,
    private val providerRepository: ProviderRepository,
    private val restoreBackupUseCase: RestoreBackupUseCase,
    private val searchHistoryRepository: SearchHistoryRepository,
    private val unloadProviderUseCase: UnloadProviderUseCase,
    private val userSessionDataStore: UserSessionDataStore,
) : ViewModel() {
    val systemPreferences = dataStoreManager
        .getSystemPrefs()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = runBlocking { dataStoreManager.getSystemPrefs().first() },
        )

    val cachedLinksCount: StateFlow<Int> = userSessionDataStore.currentUserId
        .filterNotNull()
        .flatMapLatest { id ->
            mediaLinksRepository.getSize(id)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0,
        )

    val searchHistoryCount =
        userSessionDataStore.currentUserId
            .filterNotNull()
            .flatMapLatest { userId ->
                searchHistoryRepository
                    .getAllItemsInFlow(ownerId = userId)
                    .map { it.size }
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = 0,
            )

    val providers = userSessionDataStore.currentUserId
        .filterNotNull()
        .flatMapLatest { userId ->
            providerRepository.getProvidersAsFlow(userId)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    val preferences = dataStoreManager
        .getUserPrefsAsFlow<DataPreferences>(UserPreferences.DATA_PREFS_KEY)
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = runBlocking {
                dataStoreManager.getUserPrefs(UserPreferences.DATA_PREFS_KEY)
            },
        )

    fun clearSearchHistory() {
        appDispatchers.ioScope.launch {
            val userId = getCurrentUserId()
            searchHistoryRepository.clearAll(userId)
        }
    }

    fun clearCacheLinks() {
        appDispatchers.ioScope.launch {
            val userId = getCurrentUserId()
            mediaLinksRepository.deleteAll(userId)
        }
    }

    fun updateUserPrefs(
        transform: suspend (t: DataPreferences) -> DataPreferences,
    ) {
        appDispatchers.ioScope.launch {
            dataStoreManager.updateUserPrefs(
                key = UserPreferences.DATA_PREFS_KEY,
                transform = transform
            )
        }
    }

    fun updateSystemPrefs(transform: suspend (t: SystemPreferences) -> SystemPreferences) {
        appDispatchers.ioScope.launch {
            dataStoreManager.updateSystemPrefs(transform)
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
            providerRepository.getProviders(userId).forEach {
                safeCall { unloadProviderUseCase(it.provider, uninstall = false) }
            }
        }

        return restoreBackupUseCase(
            uri = uri,
            options = options,
        ).first { state -> state is BackupState.Success || state is BackupState.Error }
            .also {
                if (includeProviders) {
                    initializeProviders(skipLoading = true).collect()
                }
            }
    }

    private suspend fun getCurrentUserId(): String {
        return userSessionDataStore.currentUserId.filterNotNull().first()
    }
}
