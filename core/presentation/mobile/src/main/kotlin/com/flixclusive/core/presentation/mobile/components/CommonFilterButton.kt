package com.flixclusive.core.presentation.mobile.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.common.locale.UiText
import com.flixclusive.core.presentation.mobile.AdaptiveTextStyle
import com.flixclusive.core.presentation.mobile.AdaptiveTextStyle.Companion.getAdaptiveTextStyle
import com.flixclusive.core.presentation.mobile.util.AdaptiveSizeUtil.getAdaptiveDp
import com.flixclusive.core.strings.R
import com.flixclusive.core.drawables.R as UiCommonR

@Composable
internal fun <T> CommonFilterButton(
    isSelected: Boolean,
    filter: T,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .height(getAdaptiveDp(29.dp))
            .widthIn(min = getAdaptiveDp(55.dp))
            .graphicsLayer {
                alpha = if (isSelected) 1f else 0.6f
            },
        contentPadding = PaddingValues(
            horizontal = 8.dp,
            vertical = 3.dp,
        ),
        shape = MaterialTheme.shapes.small,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AnimatedVisibility(isSelected) {
                AdaptiveIcon(
                    painter = painterResource(UiCommonR.drawable.check),
                    contentDescription = stringResource(R.string.sort_icon_content_desc),
                    tint = MaterialTheme.colorScheme.onSurface,
                    dp = 14.dp,
                )
            }

            Text(
                text = filter.toReadableString(),
                style = getAdaptiveTextStyle(
                    style = AdaptiveTextStyle.SemiEmphasized(),
                    size = 12.sp,
                ),
                color = MaterialTheme.colorScheme.onSurface.copy(0.8f),
            )
        }
    }
}

@Composable
private fun <T> T.toReadableString(): String {
    return when (this) {
        is String -> this
        is UiText -> this.asString()
        else -> this.toString()
    }
}
