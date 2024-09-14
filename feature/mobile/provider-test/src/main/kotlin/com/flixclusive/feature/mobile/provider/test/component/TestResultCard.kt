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
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.util.CustomClipboardManager.Companion.rememberClipboardManager
import com.flixclusive.core.ui.common.util.DummyDataForPreview.getDummyProviderData
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.core.ui.mobile.util.getFeedbackOnLongPress
import com.flixclusive.core.locale.UiText
import com.flixclusive.domain.provider.test.ProviderTestCaseOutput
import com.flixclusive.domain.provider.test.ProviderTestResult
import com.flixclusive.domain.provider.test.TestStatus
import kotlin.random.Random
import kotlin.time.Duration
import com.flixclusive.core.locale.R as LocaleR

private val ButtonHeight = 40.dp
private val CardShape = RoundedCornerShape(8.dp)
private val ContentPadding = PaddingValues(
    vertical = 10.dp,
    horizontal = 16.dp
)

@Composable
internal fun TestResultCard(
    modifier: Modifier = Modifier,
    testResult: ProviderTestResult,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    showFullLog: (ProviderTestCaseOutput) -> Unit
) {
    val clipboardManager = rememberClipboardManager()
    val maxContentHeight = when(isExpanded) {
        true -> Dp.Unspecified
        false -> 0.dp
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.TopCenter
    ) {
        TestResultCardContent(
            modifier = Modifier
                .animateContentSize(
                    animationSpec = tween(durationMillis = 100)
                )
                .heightIn(
                    min = ButtonHeight * 2,
                    max = maxContentHeight
                ),
            testResult = testResult,
            showFullLog = showFullLog,
            onCopyFullLog = { clipboardManager.setText(it) }
        )

        TestResultCardHeader(
            testScore = testResult.score,
            providerName = testResult.provider.name,
            isExpanded = isExpanded,
            onToggle = onToggle
        )
    }
}

@Composable
private fun TestResultCardHeader(
    isExpanded: Boolean,
    testScore: String,
    providerName: String,
    onToggle: () -> Unit
) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    val coolGradient = Brush.horizontalGradient(
        0F to MaterialTheme.colorScheme.tertiary,
        0.7F to MaterialTheme.colorScheme.primary
    )

    Button(
        onClick = onToggle,
        enabled = true,
        shape = CardShape,
        colors = ButtonDefaults.buttonColors(
            contentColor = LocalContentColor.current,
            containerColor = Color.Transparent
        ),
        contentPadding = ContentPadding,
        modifier = Modifier
            .heightIn(ButtonHeight)
            .shadow(
                elevation = 1.dp,
                shape = CardShape,
                clip = true,
                spotColor = Color.Transparent
            )
            .drawBehind {
                drawRect(surfaceColor)
                drawRect(coolGradient, alpha = 0.15F)
            }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = testScore,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    color = LocalContentColor.current.onMediumEmphasis()
                ),
            )

            Text(
                modifier = Modifier.weight(1F),
                text = providerName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp
                )
            )

            AnimatedContent(
                targetState = isExpanded,
                label = ""
            ) {
                val icon = if (it) {
                    Icons.Rounded.KeyboardArrowUp
                } else Icons.Rounded.KeyboardArrowDown

                Icon(
                    imageVector = icon,
                    contentDescription = stringResource(id = LocaleR.string.expand_card_icon_content_desc),
                    tint = LocalContentColor.current.onMediumEmphasis(0.8F)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TestResultCardContent(
    modifier: Modifier = Modifier,
    testResult: ProviderTestResult,
    showFullLog: (ProviderTestCaseOutput) -> Unit,
    onCopyFullLog: (String) -> Unit
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
                .padding(top = extraCutOutPadding + 4.dp)
        ) {
            for (i in testResult.outputs.indices) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (i != 0 && testResult.outputs.isNotEmpty()) {
                        HorizontalDivider(
                            thickness = 0.5.dp,
                            color = LocalContentColor.current.onMediumEmphasis(0.4F),
                            modifier = Modifier.padding(horizontal = horizontalPadding)
                        )
                    }

                    val output = testResult.outputs[i]
                    val otherLabels = getFullLogOtherLabels(
                        provider = testResult.provider,
                        testCaseOutput = output
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
                                                ?: context.getString(LocaleR.string.no_full_log)
                                        )
                                    )
                                }
                            )
                            .padding(
                                vertical = 5.dp,
                                horizontal = horizontalPadding
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun TestOutputLog(
    modifier: Modifier = Modifier,
    output: ProviderTestCaseOutput,
) {
    val context = LocalContext.current

    val shortLog = remember(output.shortLog) {
        if (output.status != TestStatus.RUNNING) {
            output.shortLog?.asString(context)
                ?: context.getString(LocaleR.string.no_short_log)
        } else context.getString(LocaleR.string.asserting)
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimatedContent(
            targetState = output.status,
            label = ""
        ) {
            when (it) {
                TestStatus.RUNNING -> {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(16.dp)
                    )
                }
                else -> {
                    Icon(
                        painter = painterResource(it.iconId),
                        contentDescription = it.toString(),
                        tint = Color(output.status.color),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

        }

        Text(
            text = output.name.asString(),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp
            )
        )

        Text(
            modifier = Modifier.weight(0.8F),
            text = shortLog,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Medium,
                color = LocalContentColor.current.onMediumEmphasis(),
                fontSize = 11.sp,
                textAlign = TextAlign.End
            )
        )
    }
}

@Preview
@Composable
private fun TestResultCardPreview() {
    val providers = List(5) { getDummyProviderData() }
    val isExpandedMap = remember {
        List(providers.size) { index: Int -> index to Random.nextBoolean() }
            .toMutableStateMap()
    }

    FlixclusiveTheme {
        Surface {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(providers) { i, data ->
                    val testResult = remember {
                        ProviderTestResult(provider = data)
                            .apply {
                                val statuses = TestStatus.entries.toTypedArray()
                                repeat(5) {
                                    add(
                                        ProviderTestCaseOutput(
                                            name = UiText.StringValue("Test Case $it"),
                                            status = statuses.random(),
                                            timeTaken = Duration.parse("1h 30m"),
                                            fullLog = UiText.StringValue("Full Log"),
                                            shortLog = UiText.StringValue("Short Log")
                                        )
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
                        showFullLog = {}
                    )
                }
            }
        }
    }
}