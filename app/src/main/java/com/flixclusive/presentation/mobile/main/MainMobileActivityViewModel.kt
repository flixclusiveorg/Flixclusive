package com.flixclusive.presentation.mobile.main

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class MainMobileActivityViewModel @Inject constructor() : ViewModel() {
    private val _isSplashActivityLaunched = MutableStateFlow(false)
    val isSplashActivityLaunched = _isSplashActivityLaunched.asStateFlow()

    private val _isConfigInitialized = MutableStateFlow(false)
    val isConfigInitialized = _isConfigInitialized.asStateFlow()

    fun onConfigSuccess() {
        _isConfigInitialized.update { true }
    }

    fun onSplashActivityLaunch() {
        _isSplashActivityLaunched.update { true }
    }
}
