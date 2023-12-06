package com.flixclusive.presentation.tv.main

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class MainTvSharedViewModel @Inject constructor(

) : ViewModel() {
    private val _state = MutableStateFlow(TvAppUiState())
    val state = _state.asStateFlow()

    fun hideSplashScreen() {
        _state.update { it.copy(isHidingSplashScreen = true) }
    }
}
