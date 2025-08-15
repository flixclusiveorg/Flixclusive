package com.flixclusive.domain.provider.testing.model

import com.flixclusive.domain.provider.util.extensions.add
import com.flixclusive.domain.provider.util.extensions.replace
import com.flixclusive.model.provider.ProviderMetadata
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Date

/**
 *
 * The data class representing the list of test outputs
 * for a specific provider.
 *
 * @property provider The provider data associated with the test outputs.
 * @property date The date when the test was run.
 * @property outputs The list of test outputs.
 * */
data class ProviderTestResult(
    val provider: ProviderMetadata,
    val date: Date = Date(),
) {
    private val _outputs = MutableStateFlow(emptyList<ProviderTestCaseResult>())
    val outputs = _outputs.asStateFlow()

    fun add(output: ProviderTestCaseResult): Int {
        _outputs.add(output)

        return _outputs.value.size - 1
    }

    fun update(
        index: Int,
        output: ProviderTestCaseResult,
    ) {
        _outputs.replace(index, output)
    }
}
