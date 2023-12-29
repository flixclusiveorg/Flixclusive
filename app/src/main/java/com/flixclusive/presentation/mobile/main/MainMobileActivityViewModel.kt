package com.flixclusive.presentation.mobile.main

import androidx.lifecycle.ViewModel
import com.flixclusive.domain.config.ConfigurationProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class MainMobileActivityViewModel @Inject constructor(
    private val configurationProvider: ConfigurationProvider
) : ViewModel() {
    private val _isConfigInitialized = MutableStateFlow(false)
    val isConfigInitialized = _isConfigInitialized.asStateFlow()

    fun onConfigSuccess() {
        _isConfigInitialized.value = true
    }

    fun initializeConfigsIfNull() {
        configurationProvider.run {
            if(homeCategoriesConfig == null || searchCategoriesConfig == null || appConfig == null || providersStatus == null) {
                _isConfigInitialized.value = false

                initialize()
            }
        }
    }
}
