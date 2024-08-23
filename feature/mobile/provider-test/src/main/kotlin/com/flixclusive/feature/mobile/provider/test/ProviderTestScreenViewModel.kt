package com.flixclusive.feature.mobile.provider.test

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastForEach
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.flixclusive.data.provider.ProviderManager
import com.flixclusive.domain.provider.test.TestProviderUseCase
import com.flixclusive.gradle.entities.ProviderData
import com.flixclusive.model.provider.id
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

    var showRepetitiveTestWarning by mutableStateOf(false)
        private set

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
        providers: ArrayList<ProviderData>,
        skipTestedProviders: Boolean = false
    ) {
        if (!showRepetitiveTestWarning) {
            providers.fastForEach {
                if (it.hasAlreadyBeenTested()) {
                    showRepetitiveTestWarning = true
                    return@startTests
                }
            }
        }

        val providersToTest = providers.apply {
            if (skipTestedProviders) {
                fastFilter { provider ->
                    !provider.hasAlreadyBeenTested()
                }
            }
        }

        showRepetitiveTestWarning = false
        testProviderUseCase(providers = providersToTest)
    }

    private fun ProviderData.hasAlreadyBeenTested(): Boolean {
        return testProviderUseCase.results.fastAny {
            it.provider.id == id
        }
    }
}
