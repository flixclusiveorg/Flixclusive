package com.flixclusive.domain.provider.test

import androidx.compose.runtime.mutableStateListOf
import com.flixclusive.model.provider.ProviderData
import java.util.Date

/**
 *
 * The data class representing the list of test outputs
 * for a specific provider.
 *
 * @property provider The provider data associated with the test outputs.
 * @property date The date when the test was run.
 * @property outputs The list of test outputs.
 * @property score The total score of the test outputs.
 * */
data class ProviderTestResult(
    val provider: ProviderData,
    val date: Date = Date(),
) {
    val outputs = mutableStateListOf<ProviderTestCaseOutput>()

    val score: String
        get() {
            val successTests = outputs.count { it.isSuccess }
            val totalTests = outputs.size

            return "$successTests/$totalTests"
        }

    fun add(output: ProviderTestCaseOutput): Int {
        outputs.add(output)

        return outputs.size - 1
    }

    fun update(index: Int, output: ProviderTestCaseOutput) {
        outputs[index] = output
    }
}