package com.flixclusive.feature.mobile.provider.test

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastFilter
import androidx.lifecycle.ViewModel
import com.flixclusive.domain.provider.test.TestProviderUseCase
import com.flixclusive.model.provider.ProviderData
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
internal class ProviderTestScreenViewModel @Inject constructor(
    val testProviderUseCase: TestProviderUseCase
) : ViewModel() {
    var showRepetitiveTestWarning by mutableStateOf(false)
        private set

    private val testResultCardsIsExpandedMap
        = mutableStateMapOf<String, Boolean>()

    internal var sortOption
        by mutableStateOf(SortOption(sort = SortOption.SortType.Date))

    fun stopTests() {
        testProviderUseCase.stop()
    }

    fun pauseTests() {
        testProviderUseCase.pause()
    }

    fun resumeTests() {
        testProviderUseCase.resume()
    }

    fun isExpanded(id: String): Boolean {
        return testResultCardsIsExpandedMap.getOrPut(id) {
            false
        }
    }

    fun toggleCard(id: String) {
        testResultCardsIsExpandedMap[id] = !(testResultCardsIsExpandedMap[id] ?: false)
    }

    fun hideRepetitiveTestWarning() {
        showRepetitiveTestWarning = false
    }

    fun startTests(
        providers: ArrayList<ProviderData>,
        skipTestedProviders: Boolean = false
    ) {
        if (
            !showRepetitiveTestWarning
            && providers.fastAny { it.hasAlreadyBeenTested() }
        ) {
            showRepetitiveTestWarning = true
            return
        }

        val providersToTest = providers.let {
            if (skipTestedProviders) {
                return@let it.fastFilter { provider ->
                    !provider.hasAlreadyBeenTested()
                }.toCollection(ArrayList())
            } else it
        }

        showRepetitiveTestWarning = false
        testProviderUseCase(providers = providersToTest)
    }

    fun clearTests() {
        testProviderUseCase.results.clear()
    }

    private fun ProviderData.hasAlreadyBeenTested(): Boolean {
        return testProviderUseCase.results.fastAny {
            it.provider.id == id
        }
    }
}
