package com.flixclusive.feature.mobile.settings.screen.providers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.datastore.DataStoreManager.Companion.getUserPrefs
import com.flixclusive.core.datastore.DataStoreManager.Companion.getUserPrefsAsFlow
import com.flixclusive.core.datastore.DataStoreManager.Companion.updateUserPrefs
import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.core.datastore.model.user.ProviderPreferences
import com.flixclusive.core.datastore.model.user.UserPreferences
import com.flixclusive.data.provider.repository.InstalledRepoRepository
import com.flixclusive.data.provider.repository.ProviderRepository
import com.flixclusive.domain.provider.usecase.manage.UnloadProviderUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltViewModel
internal class ProvidersTweakViewModel @Inject constructor(
    private val userSessionDataStore: UserSessionDataStore,
    private val dataStoreManager: DataStoreManager,
    private val providerRepository: ProviderRepository,
    private val unloadProviderUseCase: UnloadProviderUseCase,
    private val appDispatchers: AppDispatchers,
    private val installedRepoRepository: InstalledRepoRepository,
) : ViewModel() {
    val providers = userSessionDataStore.currentUserId
        .filterNotNull()
        .flatMapLatest { userId ->
            providerRepository.getProvidersAsFlow(userId)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    val repositories = userSessionDataStore.currentUserId
        .filterNotNull()
        .flatMapLatest { userId ->
            installedRepoRepository.getAllAsFlow(userId)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    val preferences = dataStoreManager
        .getUserPrefsAsFlow<ProviderPreferences>(UserPreferences.PROVIDER_PREFS_KEY)
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = runBlocking {
                dataStoreManager.getUserPrefs(UserPreferences.PROVIDER_PREFS_KEY)
            },
        )

    fun updateUserPrefs(
        transform: suspend (t: ProviderPreferences) -> ProviderPreferences,
    ) {
        appDispatchers.ioScope.launch {
            dataStoreManager.updateUserPrefs(
                key = UserPreferences.PROVIDER_PREFS_KEY,
                transform = transform
            )
        }
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
            providerRepository.getProviders(userId).forEach {
                unloadProviderUseCase(it.provider)
            }
        }
    }

    private suspend fun getCurrentUserId(): String {
        return userSessionDataStore.currentUserId.filterNotNull().first()
    }
}
