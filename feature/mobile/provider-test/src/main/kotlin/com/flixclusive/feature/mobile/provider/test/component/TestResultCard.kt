package com.flixclusive.feature.mobile.provider.test.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.toMutableStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flixclusive.core.common.locale.UiText
import com.flixclusive.core.presentation.common.util.CustomClipboardManager.Companion.rememberClipboardManager
import com.flixclusive.core.presentation.common.util.DummyDataForPreview.getDummyProviderMetadata
import com.flixclusive.core.presentation.mobile.components.AdaptiveIcon
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.core.presentation.mobile.util.AdaptiveSizeUtil.getAdaptiveDp
import com.flixclusive.core.presentation.mobile.util.AdaptiveTextStyle.asAdaptiveTextStyle
import com.flixclusive.core.presentation.mobile.util.getFeedbackOnLongPress
import com.flixclusive.domain.provider.testing.model.ProviderTestCaseResult
import com.flixclusive.domain.provider.testing.model.ProviderTestResult
import com.flixclusive.domain.provider.testing.model.TestStatus
import com.flixclusive.model.provider.ProviderMetadata
import kotlin.random.Random
import kotlin.time.Duration
import com.flixclusive.core.strings.R as LocaleR

private val ButtonHeight = 40.dp
private val CardShape = RoundedCornerShape(8.dp)
private val ContentPadding = PaddingValues(
    vertical = 10.dp,
    horizontal = 16.dp,
)

@Composable
internal fun TestResultCard(
    testResult: ProviderTestResult,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    showFullLog: (ProviderTestCaseResult) -> Unit,
    modifier: Modifier = Modifier,
) {
    val clipboardManager = rememberClipboardManager()
    val maxContentHeight = when (isExpanded) {
        true -> Dp.Unspecified
        false -> 0.dp
    }

    val testCaseResults by testResult.outputs.collectAsStateWithLifecycle()

    Box(
        modifier = modifier,
        contentAlignment = Alignment.TopCenter,
    ) {
        TestResultCardContent(
            showFullLog = showFullLog,
            onCopyFullLog = { clipboardManager.setText(it) },
            provider = testResult.provider,
            testCaseResults = testCaseResults,
            modifier = Modifier
                .animateContentSize(
                    animationSpec = tween(durationMillis = 100),
                ).heightIn(
                    min = ButtonHeight * 2,
                    max = maxContentHeight,
                ),
        )

        TestResultCardHeader(
            testScore = testCaseResults.getScore(),
            providerName = testResult.provider.name,
            isExpanded = isExpanded,
            onToggle = onToggle,
        )
    }
}

@Composable
private fun TestResultCardHeader(
    isExpanded: Boolean,
    testScore: String,
    providerName: String,
    onToggle: () -> Unit,
) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    val coolGradient = Brush.horizontalGradient(
        0F to MaterialTheme.colorScheme.tertiary,
        0.7F to MaterialTheme.colorScheme.primary,
    )

    Button(
        onClick = onToggle,
        enabled = true,
        shape = CardShape,
        colors = ButtonDefaults.buttonColors(
            contentColor = LocalContentColor.current,
            containerColor = Color.Transparent,
        ),
        contentPadding = ContentPadding,
        modifier = Modifier
            .heightIn(ButtonHeight)
            .shadow(
                elevation = 1.dp,
                shape = CardShape,
                clip = true,
                spotColor = Color.Transparent,
            ).drawBehind {
                drawRect(surfaceColor)
                drawRect(coolGradient, alpha = 0.15F)
            },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = testScore,
                fontWeight = FontWeight.Medium,
                color = LocalContentColor.current.copy(0.6f),
                style = MaterialTheme.typography.labelLarge.asAdaptiveTextStyle(12.sp),
            )

            Text(
                modifier = Modifier.weight(1F),
                text = providerName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Medium,
                style = MaterialTheme.typography.titleMedium.asAdaptiveTextStyle(16.sp),
            )

            AnimatedContent(
                targetState = isExpanded,
                label = "",
            ) {
                val icon = if (it) {
                    Icons.Rounded.KeyboardArrowUp
                } else {
                    Icons.Rounded.KeyboardArrowDown
                }

                AdaptiveIcon(
                    imageVector = icon,
                    contentDescription = stringResource(id = LocaleR.string.expand_card_icon_content_desc),
                    tint = LocalContentColor.current.copy(0.8F),
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TestResultCardContent(
    provider: ProviderMetadata,
    testCaseResults: List<ProviderTestCaseResult>,
    showFullLog: (ProviderTestCaseResult) -> Unit,
    onCopyFullLog: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val extraCutOutPadding = ButtonHeight.times(0.15F)
    val horizontalPadding = ContentPadding.calculateLeftPadding(LocalLayoutDirection.current)

    val context = LocalContext.current
    val hapticFeedback = getFeedbackOnLongPress()

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = ButtonHeight.minus(extraCutOutPadding)),
        shape = CardShape,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = ContentPadding.calculateTopPadding())
                .padding(top = extraCutOutPadding + 4.dp),
        ) {
            for (i in testCaseResults.indices) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (i != 0 && testCaseResults.isNotEmpty()) {
                        HorizontalDivider(
                            thickness = 0.5.dp,
                            color = LocalContentColor.current.copy(0.4F),
                            modifier = Modifier.padding(horizontal = horizontalPadding),
                        )
                    }

                    val output = testCaseResults[i]
                    val otherLabels = getFullLogOtherLabels(
                        provider = provider,
                        testCaseOutput = output,
                    )

                    TestOutputLog(
                        output = output,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.extraSmall)
                            .combinedClickable(
                                enabled = output.status != TestStatus.RUNNING,
                                onClick = { showFullLog(output) },
                                onLongClick = {
                                    hapticFeedback()
                                    onCopyFullLog(
                                        formatFullLog(
                                            testName = output.name.asString(context),
                                            otherLabels = otherLabels,
                                            fullLog = output.fullLog?.asString(context)
                                                ?: context.getString(LocaleR.string.no_full_log),
                                        ),
                                    )
                                },
                            ).padding(
                                vertical = 5.dp,
                                horizontal = horizontalPadding,
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun TestOutputLog(
    output: ProviderTestCaseResult,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    val shortLog = remember(output.shortLog) {
        if (output.status != TestStatus.RUNNING) {
            output.shortLog?.asString(context)
                ?: context.getString(LocaleR.string.no_short_log)
        } else {
            context.getString(LocaleR.string.asserting)
        }
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AnimatedContent(
            targetState = output.status,
            label = "",
        ) {
            when (it) {
                TestStatus.RUNNING -> {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(getAdaptiveDp(16.dp)),
                    )
                }

                else -> {
                    AdaptiveIcon(
                        painter = painterResource(it.iconId),
                        contentDescription = it.toString(),
                        tint = Color(output.status.color),
                        dp = 18.dp,
                    )
                }
            }
        }

        Text(
            text = output.name.asString(),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.Medium,
            style = MaterialTheme.typography.titleMedium.asAdaptiveTextStyle(13.sp),
        )

        Text(
            modifier = Modifier.weight(0.8F),
            text = shortLog,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.Medium,
            color = LocalContentColor.current.copy(0.6f),
            textAlign = TextAlign.End,
            style = MaterialTheme.typography.titleMedium.asAdaptiveTextStyle(11.sp),
        )
    }
}

/**
 * Extension to get the test score in the format of "X/Y" where X
 * is the number of passed tests and Y is the total number of tests.
 * */
private fun List<ProviderTestCaseResult>.getScore(): String {
    val passedTests = count { it.isSuccess }

    return "$passedTests/$size"
}

@Preview
@Composable
private fun TestResultCardBasePreview() {
    val providers = List(5) { getDummyProviderMetadata() }
    val isExpandedMap = remember {
        List(providers.size) { index: Int -> index to Random.nextBoolean() }
            .toMutableStateMap()
    }

    FlixclusiveTheme {
        Surface {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                itemsIndexed(providers) { i, data ->
                    val testResult = remember {
                        ProviderTestResult(provider = data)
                            .apply {
                                val statuses = TestStatus.entries.toTypedArray()
                                repeat(5) {
                                    add(
                                        ProviderTestCaseResult(
                                            name = UiText.StringValue("Test Case $it"),
                                            status = statuses.random(),
                                            timeTaken = Duration.parse("1h 30m"),
                                            fullLog = UiText.StringValue("Full Log"),
                                            shortLog = UiText.StringValue("Short Log"),
                                        ),
                                    )
                                }
                            }
                    }
                    TestResultCard(
                        isExpanded = isExpandedMap[i] ?: true,
                        testResult = testResult,
                        onToggle = {
                            isExpandedMap[i] = !(isExpandedMap[i] ?: true)
                        },
                        showFullLog = {},
                    )
                }
            }
        }
    }
}

@Preview(device = "spec:parent=pixel_5,orientation=landscape")
@Composable
private fun TestResultCardCompactLandscapePreview() {
    TestResultCardBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=portrait")
@Composable
private fun TestResultCardMediumPortraitPreview() {
    TestResultCardBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=landscape")
@Composable
private fun TestResultCardMediumLandscapePreview() {
    TestResultCardBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=portrait")
@Composable
private fun TestResultCardExtendedPortraitPreview() {
    TestResultCardBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=landscape")
@Composable
private fun TestResultCardExtendedLandscapePreview() {
    TestResultCardBasePreview()
}
