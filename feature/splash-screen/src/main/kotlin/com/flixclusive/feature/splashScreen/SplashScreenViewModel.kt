package com.flixclusive.feature.splashScreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.datastore.AppSettingsManager
import com.flixclusive.core.util.common.resource.Resource
import com.flixclusive.data.configuration.AppConfigurationManager
import com.flixclusive.domain.home.HomeItemsProviderUseCase
import com.flixclusive.domain.home.MINIMUM_HOME_ITEMS
import com.flixclusive.domain.updater.AppUpdateCheckerUseCase
import com.flixclusive.domain.updater.ProviderUpdaterUseCase
import com.flixclusive.model.datastore.AppSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

internal sealed class SplashScreenUiState {
    data object Loading: SplashScreenUiState()
    data object Okay: SplashScreenUiState()
    data object Failure: SplashScreenUiState()
}

@HiltViewModel
internal class SplashScreenViewModel @Inject constructor(
    homeItemsProviderUseCase: HomeItemsProviderUseCase,
    appConfigurationManager: AppConfigurationManager,
    private val appSettingsManager: AppSettingsManager,
    val appUpdateCheckerUseCase: AppUpdateCheckerUseCase,
    private val providerUpdaterUseCase: ProviderUpdaterUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow<SplashScreenUiState>(SplashScreenUiState.Loading)
    val uiState = _uiState.asStateFlow()

    val configurationStatus = appConfigurationManager.configurationStatus

    val appSettings = appSettingsManager
        .appSettings
        .data
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = appSettingsManager.localAppSettings
        )

    init {
        viewModelScope.launch {
            launch {
                appConfigurationManager.configurationStatus.collectLatest {
                    if (it is Resource.Success) {
                        homeItemsProviderUseCase()
                    }
                }
            }

            launch {
                homeItemsProviderUseCase.rowItems
                    .combine(homeItemsProviderUseCase.initializationStatus) { items, status ->
                        items to status
                    }.collectLatest { (items, status) ->
                        _uiState.update {
                            when {
                                status is Resource.Success && items.size >= MINIMUM_HOME_ITEMS -> SplashScreenUiState.Okay
                                status is Resource.Failure -> SplashScreenUiState.Failure
                                else -> it
                            }
                        }
                    }
            }

            launch {
                providerUpdaterUseCase.checkForUpdates(notify = true)
            }
        }
    }

    fun updateSettings(newAppSettings: AppSettings) {
        viewModelScope.launch {
            appSettingsManager.updateSettings(newAppSettings)
        }
    }
}