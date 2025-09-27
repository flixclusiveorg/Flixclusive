package com.flixclusive.feature.mobile.provider.test

import android.content.Context
import androidx.compose.runtime.Stable
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastFilter
import androidx.lifecycle.ViewModel
import com.flixclusive.core.strings.R
import com.flixclusive.data.provider.repository.ProviderRepository
import com.flixclusive.domain.provider.testing.ProviderTester
import com.flixclusive.model.provider.ProviderMetadata
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
internal class ProviderTestScreenViewModel
    @Inject
    constructor(
        private val providerTester: ProviderTester,
        private val providerRepository: ProviderRepository,
    ) : ViewModel() {
        val results = providerTester.results
        val testStage = providerTester.testStage
        val testJobState = providerTester.testJobState
        val filmOnTest = providerTester.filmOnTest

        fun stopTests() {
            providerTester.stop()
        }

        fun pauseTests() {
            providerTester.pause()
        }

        fun resumeTests() {
            providerTester.resume()
        }

        fun startTests(
            providers: ArrayList<ProviderMetadata>,
            skipTestedProviders: Boolean = false,
            testAgainIfTested: Boolean = false,
        ): StartTestResult {
            // Show warning if any of the selected providers have already been tested
            if (!testAgainIfTested && !skipTestedProviders && providers.fastAny(::isAlreadyBeenTested)) {
                return StartTestResult.SHOW_WARNING
            }

            val providersToTest = providers.let {
                // If no providers were selected, test all providers
                if (it.isEmpty()) {
                    ArrayList(providerRepository.getProviders())
                } else if (skipTestedProviders) {
                    it
                        .fastFilter { metadata -> !isAlreadyBeenTested(metadata) }
                        .toCollection(ArrayList())
                } else {
                    it
                }
            }

            providerTester.start(providers = providersToTest)

            return StartTestResult.STARTED
        }

        fun clearTests() {
            providerTester.clear()
        }

        private fun isAlreadyBeenTested(metadata: ProviderMetadata): Boolean {
            return providerTester.results.value.fastAny { it.provider.id == metadata.id }
        }
    }

internal enum class StartTestResult {
    STARTED,
    SHOW_WARNING,
}

@Stable
internal data class SortOption(
    val sort: SortType,
    val ascending: Boolean = true,
) {
    enum class SortType {
        Name,
        Date,
        Score,
        ;

        fun toString(context: Context): String {
            return when (this) {
                Name -> context.getString(R.string.sort_name)
                Date -> context.getString(R.string.sort_date)
                Score -> context.getString(R.string.sort_score)
            }
        }
    }
}
