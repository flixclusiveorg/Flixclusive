package com.flixclusive.domain.provider.test

import androidx.annotation.DrawableRes
import com.flixclusive.core.locale.UiText
import com.flixclusive.domain.provider.test.TestStatus.FAILURE
import com.flixclusive.domain.provider.test.TestStatus.NOT_IMPLEMENTED
import com.flixclusive.domain.provider.test.TestStatus.RUNNING
import com.flixclusive.domain.provider.test.TestStatus.SUCCESS
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import com.flixclusive.core.ui.common.R as UiCommonR

/**
 *
 * The data class for output of a single provider test.
 *
 * @property status The status of the test output.
 * @property name The name of the output.
 * @property timeTaken The total measured time of the test.
 * @property shortLog The short output of the test.
 * @property fullLog The full output of the test.
 *
 * @see TestStatus
 * */
data class ProviderTestCaseOutput(
    val status: TestStatus,
    val name: UiText,
    val timeTaken: Duration = 0.seconds,
    val fullLog: UiText? = null,
    val shortLog: UiText? = null
) {
    val isSuccess: Boolean
        get() = status == SUCCESS
}

/**
 *
 * The status of the test output.
 *
 * @see RUNNING
 * @see SUCCESS
 * @see FAILURE
 * @see NOT_IMPLEMENTED
 *
 * @see ProviderTestCaseOutput
 * */
enum class TestStatus(
    @DrawableRes val iconId: Int,
    val color: Long = -1L
) {
    RUNNING(iconId = -1),
    SUCCESS(
        iconId = UiCommonR.drawable.check,
        color = 0xFF30FF1F
    ),
    FAILURE(
        iconId = UiCommonR.drawable.round_error_outline_24,
        color = 0xFFFF2C2C
    ),
    NOT_IMPLEMENTED(
        iconId = UiCommonR.drawable.warning_outline,
        color = 0xFFFFD54F
    );
}