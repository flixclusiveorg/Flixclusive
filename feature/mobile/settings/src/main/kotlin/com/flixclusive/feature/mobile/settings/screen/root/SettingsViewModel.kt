package com.flixclusive.feature.mobile.settings.screen.root

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.datastore.AppSettingsManager
import com.flixclusive.data.search_history.SearchHistoryRepository
import com.flixclusive.data.user.UserRepository
import com.flixclusive.domain.provider.GetMediaLinksUseCase
import com.flixclusive.domain.user.UserSessionManager
import com.flixclusive.model.datastore.AppSettings
import com.flixclusive.model.datastore.AppSettingsProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class SettingsViewModel @Inject constructor(
    val userSessionManager: UserSessionManager,
    private val userRepository: UserRepository,
    private val appSettingsManager: AppSettingsManager,
    private val searchHistoryRepository: SearchHistoryRepository,
    private val getMediaLinksUseCase: GetMediaLinksUseCase
) : ViewModel() {
    val searchHistoryCount = userSessionManager.currentUser
        .filterNotNull()
        .flatMapLatest { user ->
            searchHistoryRepository.getAllItemsInFlow(ownerId = user.id)
                .map { it.size }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    val appSettings = appSettingsManager.appSettings.data
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = appSettingsManager.cachedAppSettings
        )

    val appSettingsProvider = appSettingsManager.providerSettings.data
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = appSettingsManager.cachedProviderSettings
        )

    val cacheLinksSize by derivedStateOf {
        getMediaLinksUseCase.cache.size
    }

    fun onChangeAppSettings(newAppSettings: AppSettings) {
        viewModelScope.launch {
            appSettingsManager.updateSettings(newAppSettings)
        }
    }

    fun onChangeAppSettingsProvider(newAppSettingsProvider: AppSettingsProvider) {
        viewModelScope.launch {
            appSettingsManager.updateProviderSettings {
                newAppSettingsProvider
            }
        }
    }

    fun clearSearchHistory() {
        viewModelScope.launch {
            val userId = userSessionManager.currentUser.value?.id
                ?: return@launch

            searchHistoryRepository.clearAll(userId)
        }
    }

    fun clearCacheLinks() {
        getMediaLinksUseCase.cache.clear()
    }
}