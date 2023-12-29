package com.flixclusive.presentation.common.viewmodels.config

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.BuildConfig
import com.flixclusive.domain.config.ConfigurationProvider
import com.flixclusive.domain.config.RemoteConfigStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppConfigurationViewModel @Inject constructor(
    private val configurationProvider: ConfigurationProvider
) : ViewModel() {
    private val _state = MutableStateFlow(AppConfigurationUiState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            configurationProvider.remoteStatus.collect { status ->
                val appConfig = configurationProvider.appConfig

                _state.update {
                    when(status) {
                        is RemoteConfigStatus.Loading -> it
                        is RemoteConfigStatus.Success -> {
                            if(appConfig!!.isMaintenance && !BuildConfig.DEBUG)
                                return@update it.copy(isMaintenance = true)

                            val isNeedingAnUpdate = if(appConfig.build != -1L) {
                                appConfig.build > BuildConfig.VERSION_CODE.toLong()
                            } else false

                            it.copy(
                                isDoneInitializing = !isNeedingAnUpdate,
                                isNeedingAnUpdate = isNeedingAnUpdate,
                                updateUrl = appConfig.updateUrl,
                                newVersion = appConfig.versionName,
                                updateInfo = appConfig.updateInfo
                            )
                        }
                        is RemoteConfigStatus.Error -> it.copy(errorMessage = status.errorMessage)
                    }
                }
            }
        }
    }

    fun checkForUpdates() {
        configurationProvider.checkForUpdates()
    }

    fun onConsumeUpdateDialog() {
        _state.update {
            it.copy(
                isDoneInitializing = true
            )
        }
    }

    fun setAllPermissionsAllowed() {
        _state.update {
            it.copy(
                allPermissionsAreAllowed = true
            )
        }
    }

    fun showTvScreen() {
        _state.update { it.copy(isShowingTvScreen = true) }
    }
}
