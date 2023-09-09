package com.flixclusive.presentation.mobile.splash_screen

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
class SplashScreenViewModel @Inject constructor(
    private val configurationProvider: ConfigurationProvider
) : ViewModel() {
    private val _state = MutableStateFlow(SplashScreenUiState())
    val state = _state.asStateFlow()
    init {
        viewModelScope.launch {
            configurationProvider.remoteStatus.collect { status ->
                val appConfig = configurationProvider.appConfig

                _state.update {
                    when(status) {
                        RemoteConfigStatus.LOADING -> it
                        RemoteConfigStatus.SUCCESS -> {
                            if(appConfig!!.isMaintenance)
                                return@update it.copy(isMaintenance = true)

                            val isNeedingAnUpdate = if(appConfig.build != -1L) {
                                appConfig.build > BuildConfig.VERSION_CODE.toLong()
                            } else false

                            it.copy(
                                isDoneInitializing = !isNeedingAnUpdate,
                                isNeedingAnUpdate = isNeedingAnUpdate,
                                updateUrl = appConfig.updateUrl
                            )
                        }
                        RemoteConfigStatus.ERROR -> it.copy(isError = true)
                    }
                }
            }
        }
    }

    fun onConsumeUpdateDialog() {
        _state.update {
            it.copy(
                isDoneInitializing = true
            )
        }
    }
}
