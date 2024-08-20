package com.flixclusive.feature.mobile.provider.test

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.flixclusive.data.provider.ProviderManager
import com.flixclusive.domain.provider.test.TestProviderUseCase
import com.flixclusive.gradle.entities.ProviderData
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ProviderTestScreenViewModel @Inject constructor(
    providerManager: ProviderManager,
    savedStateHandle: SavedStateHandle,
    val testProviderUseCase: TestProviderUseCase
) : ViewModel() {
//    val providerData = savedStateHandle.navArgs<ProviderTestScreenNavArgs>().providers
//    val providerInstance = providerManager.providers[providerData.name]

    fun stopTests() {
        testProviderUseCase.stop()
    }

    fun pauseTests() {
        testProviderUseCase.pause()
    }

    fun resumeTests() {
        testProviderUseCase.resume()
    }

    fun startTests(
        providers: ArrayList<ProviderData>
    ) {
        testProviderUseCase(providers = providers)
    }
}
