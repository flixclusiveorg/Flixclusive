package com.flixclusive.feature.mobile.settings

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.datastore.AppSettingsManager
import com.flixclusive.data.search_history.SearchHistoryRepository
import com.flixclusive.data.user.UserRepository
import com.flixclusive.domain.provider.GetMediaLinksUseCase
import com.flixclusive.model.datastore.AppSettings
import com.flixclusive.model.datastore.AppSettingsProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class SettingsViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val appSettingsManager: AppSettingsManager,
    private val searchHistoryRepository: SearchHistoryRepository,
    private val getMediaLinksUseCase: GetMediaLinksUseCase
) : ViewModel() {
    val searchHistoryCount = searchHistoryRepository.getAllItemsInFlow()
        .map { it.size }
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
            searchHistoryRepository.clearAll()
        }
    }

    fun clearCacheLinks() {
        getMediaLinksUseCase.cache.clear()
    }
}