package com.flixclusive.feature.mobile.provider.test

import android.content.Context
import androidx.compose.runtime.Stable
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastFilter
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.strings.R
import com.flixclusive.data.database.session.UserSessionManager
import com.flixclusive.data.provider.repository.ProviderRepository
import com.flixclusive.domain.provider.testing.ProviderTester
import com.flixclusive.model.provider.ProviderMetadata
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class ProviderTestScreenViewModel @Inject constructor(
    private val providerTester: ProviderTester,
    private val providerRepository: ProviderRepository,
    private val userSessionManager: UserSessionManager
) : ViewModel() {
    val results = providerTester.results
    val testStage = providerTester.testStage
    val testJobState = providerTester.testJobState
    val filmOnTest = providerTester.filmOnTest

    private val _warnDuplicateTests = MutableSharedFlow<Boolean>()
    val warnDuplicateTests = _warnDuplicateTests.asSharedFlow()

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
    ) {
        viewModelScope.launch {
            val providersToTest = providers.let {
                when {
                    it.isEmpty() -> ArrayList(getAllProviders())
                    skipTestedProviders -> {
                        it
                            .fastFilter { metadata -> !isAlreadyBeenTested(metadata) }
                            .toCollection(ArrayList())
                    }
                    else -> it
                }
            }

            // Show warning if any of the selected providers have already been tested
            if (!testAgainIfTested && !skipTestedProviders && providersToTest.fastAny(::isAlreadyBeenTested)) {
                _warnDuplicateTests.emit(true)
                return@launch
            }

            providerTester.start(providers = providersToTest)
        }
    }

    fun clearTests() {
        providerTester.clear()
    }

    private fun isAlreadyBeenTested(metadata: ProviderMetadata): Boolean {
        return providerTester.results.value.fastAny { it.provider.id == metadata.id }
    }

    private suspend fun getAllProviders(): List<ProviderMetadata> {
        val userId = userSessionManager.currentUser.filterNotNull().first().id
        val installedProviders = providerRepository.getInstalledProviders(userId)

        return installedProviders.mapNotNull { providerRepository.getMetadata(it.id) }
    }
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
