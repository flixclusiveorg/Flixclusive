package com.flixclusive.presentation.mobile.screens.preferences.providers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.domain.preferences.AppSettingsManager
import com.flixclusive.domain.repository.ProvidersRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProvidersListViewModel @Inject constructor(
    private val providersRepository: ProvidersRepository,
    appSettingsManager: AppSettingsManager,
) : ViewModel() {
    val providers = providersRepository.providers
    private val appSettings = appSettingsManager.appSettings
        .data
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = appSettingsManager.localAppSettings
        )

    fun onMove(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            providersRepository.swap(appSettings.value, fromIndex, toIndex)
        }
    }

    fun toggleProvider(index: Int) {
        viewModelScope.launch {
            providersRepository.toggleUsage(appSettings.value, index)
        }
    }
}
