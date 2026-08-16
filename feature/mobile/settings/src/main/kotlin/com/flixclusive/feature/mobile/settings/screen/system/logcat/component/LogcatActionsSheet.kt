package com.flixclusive.feature.mobile.settings.screen.system.logcat.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flixclusive.core.presentation.mobile.components.AdaptiveIcon
import com.flixclusive.core.presentation.mobile.components.material3.CommonBottomSheet
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.core.presentation.mobile.util.AdaptiveSizeUtil.getAdaptiveDp
import com.flixclusive.feature.mobile.settings.R
import com.flixclusive.core.drawables.R as UiCommonR

@Composable
internal fun LogcatActionsSheet(
    onDismissRequest: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CommonBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(bottom = 16.dp)) {
            SheetAction(
                iconId = UiCommonR.drawable.round_content_copy_24,
                label = stringResource(R.string.logcat_copy),
                onClick = onCopy,
            )

            SheetAction(
                iconId = UiCommonR.drawable.share,
                label = stringResource(R.string.logcat_share),
                onClick = onShare,
            )

            SheetAction(
                iconId = UiCommonR.drawable.delete,
                label = stringResource(R.string.logcat_clear),
                tint = MaterialTheme.colorScheme.error,
                onClick = onClear,
            )
        }
    }
}

@Composable
private fun SheetAction(
    @DrawableRes iconId: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = getAdaptiveDp(52.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp),
    ) {
        AdaptiveIcon(
            painter = painterResource(iconId),
            contentDescription = null,
            tint = tint,
            dp = 20.dp,
        )

        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = tint,
        )
    }
}

@Preview
@Composable
private fun LogcatActionsSheetPreview() {
    FlixclusiveTheme {
        Surface {
            LogcatActionsSheet(
                onDismissRequest = {},
                onCopy = {},
                onShare = {},
                onClear = {},
            )
        }
    }
}
