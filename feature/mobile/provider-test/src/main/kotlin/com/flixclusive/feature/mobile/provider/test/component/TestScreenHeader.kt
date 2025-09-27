package com.flixclusive.feature.mobile.provider.test.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.presentation.common.util.DummyDataForPreview.getDummyProviderMetadata
import com.flixclusive.core.presentation.mobile.AdaptiveTextStyle.asAdaptiveTextStyle
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.core.presentation.mobile.util.AdaptiveSizeUtil.getAdaptiveDp
import com.flixclusive.domain.provider.testing.TestStage
import com.flixclusive.core.strings.R as LocaleR

@Composable
internal fun TestScreenHeader(
    stage: TestStage,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    Box(
        modifier = modifier
            .heightIn(min = getAdaptiveDp(295.dp))
            .fillMaxWidth(),
        contentAlignment = Alignment.TopCenter,
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
                    HeaderLabels(stage = stage)
                }

                false -> {
                    Box(
                        modifier = Modifier.matchParentSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stage.toString(context),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.headlineMedium.asAdaptiveTextStyle(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderLabels(
    stage: TestStage,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val testingLabel = remember {
        context.getString(LocaleR.string.currently_testing).uppercase()
    }
    val testingStageLabel = remember {
        context.getString(LocaleR.string.stage).uppercase()
    }

    val stageLabelColor by animateColorAsState(
        targetValue = when (stage) {
            is TestStage.Idle -> LocalContentColor.current
            else -> MaterialTheme.colorScheme.tertiary
        },
    )

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(getAdaptiveDp(50.dp)),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = testingLabel,
                fontWeight = FontWeight.Normal,
                letterSpacing = 1.sp,
                color = LocalContentColor.current.copy(0.6f),
                style = MaterialTheme.typography.titleMedium.asAdaptiveTextStyle(),
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
                            SizeTransform(clip = false),
                        )
                    },
                ) { providerName ->
                    Text(
                        text = providerName,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.headlineMedium.asAdaptiveTextStyle(),
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(
                text = testingStageLabel,
                fontWeight = FontWeight.Medium,
                color = LocalContentColor.current.copy(0.8F),
                style = MaterialTheme.typography.labelLarge.asAdaptiveTextStyle(12.sp),
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
                        SizeTransform(clip = false),
                    )
                },
            ) {
                Text(
                    text = it.toString(context),
                    fontWeight = FontWeight.Medium,
                    color = stageLabelColor,
                    style = MaterialTheme.typography.titleMedium.asAdaptiveTextStyle(16.sp),
                )
            }
        }
    }
}

@Preview
@Composable
private fun TestScreenHeaderBasePreview(stage: TestStage = TestStage.Stage2(remember { getDummyProviderMetadata() })) {
    FlixclusiveTheme {
        Surface {
            TestScreenHeader(stage = stage)
        }
    }
}

@Preview(device = "spec:parent=pixel_5,orientation=landscape")
@Composable
private fun TestScreenHeaderCompactLandscapePreview() {
    TestScreenHeaderBasePreview(stage = TestStage.Stage1(remember { getDummyProviderMetadata() }))
}

@Preview(device = "spec:parent=medium_tablet,orientation=portrait")
@Composable
private fun TestScreenHeaderMediumPortraitPreview() {
    TestScreenHeaderBasePreview(
        stage = TestStage.Idle,
    )
}

@Preview(device = "spec:parent=medium_tablet,orientation=landscape")
@Composable
private fun TestScreenHeaderMediumLandscapePreview() {
    TestScreenHeaderBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=portrait")
@Composable
private fun TestScreenHeaderExtendedPortraitPreview() {
    TestScreenHeaderBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=landscape")
@Composable
private fun TestScreenHeaderExtendedLandscapePreview() {
    TestScreenHeaderBasePreview()
}
