package com.flixclusive.feature.mobile.provider.test.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.util.DummyDataForPreview.getDummyProviderData
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.domain.provider.test.TestStage
import com.flixclusive.domain.provider.test.TestStage.Idle.Companion.isIdle
import com.flixclusive.core.locale.R as LocaleR

private val HeaderLabelSpacing = 50.dp

@Composable
internal fun TestScreenHeader(
    modifier: Modifier = Modifier,
    stage: TestStage,
) {
    Box(
        modifier = modifier
            .heightIn(min = 295.dp)
            .fillMaxWidth(),
        contentAlignment = Alignment.TopCenter
    ) {
        AnimatedContent(
            targetState = stage.providerOnTest != null && !stage.isIdle,
            transitionSpec = {
                fadeIn(animationSpec = tween(220, delayMillis = 90)) togetherWith
                        fadeOut(animationSpec = tween(90))
            },
            label = "",
        ) {
            when (it) {
                true -> {
                    HeaderLabels(
                        modifier = Modifier.padding(
                            top = 100.dp,
                            bottom = HeaderLabelSpacing
                        ),
                        stage = stage
                    )
                }

                false -> {
                    Box(
                        modifier = Modifier.height(295.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(LocaleR.string.provider_test_stage_idle),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                textAlign = TextAlign.Center
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderLabels(
    modifier: Modifier = Modifier,
    stage: TestStage,
) {
    val context = LocalContext.current
    val testingLabel = remember {
        context.getString(LocaleR.string.currently_testing).uppercase()
    }
    val testingStageLabel = remember {
        context.getString(LocaleR.string.stage).uppercase()
    }

    val stageLabelColor = when (stage) {
        is TestStage.Done -> Color(0xFF30FF1F)
        is TestStage.Idle -> LocalContentColor.current
        else -> MaterialTheme.colorScheme.tertiary
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(HeaderLabelSpacing),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = testingLabel,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Normal,
                    letterSpacing = 1.sp,
                    color = LocalContentColor.current.onMediumEmphasis()
                ),
            )

            if (stage.providerOnTest?.name != null) {
                AnimatedContent(
                    targetState = stage.providerOnTest!!.name,
                    label = "",
                    transitionSpec = {
                        if (targetState > initialState) {
                            fadeIn() + slideInHorizontally { it } togetherWith
                                    fadeOut() + slideOutHorizontally { -it }
                        } else {
                            fadeIn() + slideInHorizontally { -it } + fadeIn() togetherWith
                                    fadeOut() + slideOutHorizontally { it }
                        }.using(
                            SizeTransform(clip = false)
                        )
                    },
                ) { providerName ->
                    Text(
                        text = providerName,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            textAlign = TextAlign.Center
                        ),
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = testingStageLabel,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    color = LocalContentColor.current.onMediumEmphasis(0.8F)
                ),
            )

            AnimatedContent(
                targetState = stage,
                label = "",
                transitionSpec = {
                    if (targetState > initialState) {
                        fadeIn() + slideInHorizontally { it } togetherWith
                                fadeOut() + slideOutHorizontally { -it }
                    } else {
                        fadeIn() + slideInHorizontally { -it } + fadeIn() togetherWith
                                fadeOut() + slideOutHorizontally { it }
                    }.using(
                        SizeTransform(clip = false)
                    )
                },
            ) {
                Text(
                    text = it.toString(context),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Medium,
                        color = stageLabelColor,
                        fontSize = 16.sp
                    ),
                )
            }
        }
    }
}

@Preview
@Composable
private fun ScreenHeaderPreview() {
    FlixclusiveTheme {
        Surface {
            TestScreenHeader(
                stage = TestStage.Stage1(getDummyProviderData())
            )
        }
    }
}

@Preview
@Composable
private fun ScreenHeaderPreview1() {
    FlixclusiveTheme {
        Surface {
            TestScreenHeader(
                stage = TestStage.Idle(null)
            )
        }
    }
}