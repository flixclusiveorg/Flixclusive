package com.flixclusive.feature.mobile.provider.test.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flixclusive.core.presentation.mobile.extensions.fillMaxAdaptiveWidth
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.core.presentation.mobile.util.AdaptiveTextStyle.asAdaptiveTextStyle
import com.flixclusive.domain.provider.testing.TestJobState
import com.flixclusive.core.strings.R as LocaleR

@Composable
private fun getBorderStroke() =
    BorderStroke(
        width = 0.8.dp,
        color = MaterialTheme.colorScheme.primary.copy(0.6f),
    )

@Composable
internal fun ButtonControllerDivider(
    testJobState: TestJobState,
    onStop: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .fillMaxWidth(),
    ) {
        AnimatedContent(
            targetState = testJobState,
            label = "",
        ) { state ->
            when (state) {
                TestJobState.PAUSED -> PausedStateButtons(onResume = onResume)
                TestJobState.IDLE -> IdleStateButtons(onStart = onStart)
                TestJobState.RUNNING -> RunningStateButtons(
                    onStop = onStop,
                    onPause = onPause,
                    modifier = Modifier.fillMaxAdaptiveWidth(
                        medium = 0.4f,
                        expanded = 0.4f,
                    ),
                )
            }
        }

        HorizontalDivider(
            color = LocalContentColor.current.copy(0.6f),
            thickness = 0.5.dp,
        )
    }
}

@Composable
private fun RunningStateButtons(
    onStop: () -> Unit,
    onPause: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        OutlinedButton(
            onClick = onPause,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.weight(1F),
        ) {
            Text(
                text = stringResource(id = LocaleR.string.pause),
                color = MaterialTheme.colorScheme.onSurface.copy(0.8F),
                fontWeight = FontWeight.Bold,
                style = LocalTextStyle.current.asAdaptiveTextStyle(),
            )
        }

        OutlinedButton(
            onClick = onStop,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.weight(1F),
        ) {
            Text(
                text = stringResource(id = LocaleR.string.stop),
                color = MaterialTheme.colorScheme.onSurface.copy(0.8F),
                fontWeight = FontWeight.Bold,
                style = LocalTextStyle.current.asAdaptiveTextStyle(),
            )
        }
    }
}

@Composable
private fun PausedStateButtons(
    modifier: Modifier = Modifier,
    onResume: () -> Unit,
) {
    Box(
        modifier = modifier,
    ) {
        OutlinedButton(
            onClick = onResume,
            shape = MaterialTheme.shapes.small,
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05F),
            ),
            border = getBorderStroke(),
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxAdaptiveWidth(
                    compact = 0.5F,
                    medium = 0.3F,
                    expanded = 0.2F,
                ),
        ) {
            Text(
                text = stringResource(id = LocaleR.string.resume),
                fontWeight = FontWeight.Bold,
                style = LocalTextStyle.current.asAdaptiveTextStyle(),
            )
        }
    }
}

@Composable
private fun IdleStateButtons(
    modifier: Modifier = Modifier,
    onStart: () -> Unit,
) {
    Box(
        modifier = modifier,
    ) {
        OutlinedButton(
            onClick = onStart,
            shape = MaterialTheme.shapes.small,
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05F),
            ),
            border = getBorderStroke(),
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxAdaptiveWidth(
                    compact = 0.5F,
                    medium = 0.3F,
                    expanded = 0.2F,
                ),
        ) {
            Text(
                text = stringResource(id = LocaleR.string.start),
                fontWeight = FontWeight.Bold,
                style = LocalTextStyle.current.asAdaptiveTextStyle(),
            )
        }
    }
}

@Preview
@Composable
private fun ButtonControllerDividerBasePreview() {
    FlixclusiveTheme {
        Surface {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ButtonControllerDivider(
                    testJobState = TestJobState.IDLE,
                    onStop = {},
                    onPause = {},
                    onResume = {},
                    onStart = {},
                )

                ButtonControllerDivider(
                    testJobState = TestJobState.RUNNING,
                    onStop = {},
                    onPause = {},
                    onResume = {},
                    onStart = {},
                )

                ButtonControllerDivider(
                    testJobState = TestJobState.PAUSED,
                    onStop = {},
                    onPause = {},
                    onResume = {},
                    onStart = {},
                )
            }
        }
    }
}

@Preview(device = "spec:parent=pixel_5,orientation=landscape")
@Composable
private fun ButtonControllerDividerCompactLandscapePreview() {
    ButtonControllerDividerBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=portrait")
@Composable
private fun ButtonControllerDividerMediumPortraitPreview() {
    ButtonControllerDividerBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=landscape")
@Composable
private fun ButtonControllerDividerMediumLandscapePreview() {
    ButtonControllerDividerBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=portrait")
@Composable
private fun ButtonControllerDividerExtendedPortraitPreview() {
    ButtonControllerDividerBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=landscape")
@Composable
private fun ButtonControllerDividerExtendedLandscapePreview() {
    ButtonControllerDividerBasePreview()
}
