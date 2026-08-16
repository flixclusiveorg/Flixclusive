package com.flixclusive.feature.mobile.settings.screen.system.logcat.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flixclusive.core.presentation.mobile.components.AdaptiveIcon
import com.flixclusive.core.presentation.mobile.components.material3.PlainTooltipBox
import com.flixclusive.core.presentation.mobile.components.material3.topbar.ActionButton
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.feature.mobile.settings.R
import com.flixclusive.core.drawables.R as UiCommonR

@Composable
internal fun LogcatFindBar(
    matchCount: Int,
    currentMatch: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasMatches = matchCount > 0

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
    ) {
        Text(
            text = if (hasMatches) {
                stringResource(R.string.logcat_match_format, currentMatch + 1, matchCount)
            } else {
                stringResource(R.string.logcat_no_matches)
            },
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
            color = LocalContentColor.current.copy(alpha = if (hasMatches) 0.8f else 0.5f),
        )

        PlainTooltipBox(description = stringResource(R.string.logcat_find_previous)) {
            ActionButton(onClick = onPrevious, enabled = hasMatches) {
                AdaptiveIcon(
                    painter = painterResource(UiCommonR.drawable.up_arrow),
                    contentDescription = stringResource(R.string.logcat_find_previous),
                    dp = 18.dp,
                )
            }
        }

        PlainTooltipBox(description = stringResource(R.string.logcat_find_next)) {
            ActionButton(onClick = onNext, enabled = hasMatches) {
                AdaptiveIcon(
                    painter = painterResource(UiCommonR.drawable.down_arrow),
                    contentDescription = stringResource(R.string.logcat_find_next),
                    dp = 18.dp,
                )
            }
        }
    }
}

@Preview
@Composable
private fun LogcatFindBarPreview() {
    FlixclusiveTheme {
        Surface {
            LogcatFindBar(
                matchCount = 17,
                currentMatch = 2,
                onPrevious = {},
                onNext = {},
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }
    }
}
