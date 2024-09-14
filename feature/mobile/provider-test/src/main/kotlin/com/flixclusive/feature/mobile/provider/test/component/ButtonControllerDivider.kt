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
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.domain.provider.test.TestJobState
import com.flixclusive.core.locale.R as LocaleR


@Composable
private fun getBorderStroke()
    = BorderStroke(
        width = 0.8.dp,
        color = MaterialTheme.colorScheme.primary.onMediumEmphasis()
    )

@Composable
internal fun ButtonControllerDivider(
    modifier: Modifier = Modifier,
    testJobState: TestJobState,
    onStop: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStart: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .fillMaxWidth()
    ) {
        AnimatedContent(
            targetState = testJobState,
            label = ""
        ) { state ->
            when (state) {
                TestJobState.PAUSED -> PausedStateButtons(onResume = onResume)
                TestJobState.RUNNING -> RunningStateButtons(
                    onStop = onStop,
                    onPause = onPause
                )
                TestJobState.IDLE -> IdleStateButtons(onStart = onStart)
            }
        }

        HorizontalDivider(
            color = LocalContentColor.current.onMediumEmphasis(),
            thickness = 0.5.dp
        )
    }
}

@Composable
private fun RunningStateButtons(
    modifier: Modifier = Modifier,
    onStop: () -> Unit,
    onPause: () -> Unit,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedButton(
            onClick = onPause,
            shape = MaterialTheme.shapes.extraSmall,
            modifier = Modifier.weight(1F)
        ) {
            Text(
                text = stringResource(id = LocaleR.string.pause),
                style = LocalTextStyle.current.copy(
                    color = MaterialTheme.colorScheme.onSurface.onMediumEmphasis(0.8F),
                    fontWeight = FontWeight.Bold
                )
            )
        }

        OutlinedButton(
            onClick = onStop,
            shape = MaterialTheme.shapes.extraSmall,
            modifier = Modifier.weight(1F)
        ) {
            Text(
                text = stringResource(id = LocaleR.string.stop),
                style = LocalTextStyle.current.copy(
                    color = MaterialTheme.colorScheme.onSurface.onMediumEmphasis(0.8F),
                    fontWeight = FontWeight.Bold
                )
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
            shape = MaterialTheme.shapes.extraSmall,
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05F),
            ),
            border = getBorderStroke(),
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.5F)
        ) {
            Text(
                text = stringResource(id = LocaleR.string.resume),
                style = LocalTextStyle.current.copy(
                    fontWeight = FontWeight.Bold
                )
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
            shape = MaterialTheme.shapes.extraSmall,
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05F),
            ),
            border = getBorderStroke(),
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.5F)
        ) {
            Text(
                text = stringResource(id = LocaleR.string.start),
                style = LocalTextStyle.current.copy(
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

@Preview
@Composable
private fun ButtonControllerDividerPreview() {
    FlixclusiveTheme {
        Surface {
            ButtonControllerDivider(
                testJobState = TestJobState.IDLE,
                onStop = {},
                onPause = {},
                onResume = {},
                onStart = {}
            )
        }
    }
}