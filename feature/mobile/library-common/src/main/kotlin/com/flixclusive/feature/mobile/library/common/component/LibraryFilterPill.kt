package com.flixclusive.feature.mobile.library.common.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flixclusive.core.presentation.mobile.components.AdaptiveIcon
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.core.presentation.mobile.util.AdaptiveSizeUtil.getAdaptiveDp
import com.flixclusive.core.presentation.mobile.util.AdaptiveTextStyle.asAdaptiveTextStyle
import com.flixclusive.feature.mobile.library.common.util.LibrarySortFilter
import com.flixclusive.core.drawables.R as UiCommonR
import com.flixclusive.core.strings.R as LocaleR

@Composable
fun LibraryFilterPill(
    isSelected: Boolean,
    filter: LibrarySortFilter,
    ascending: Boolean,
    onToggleDirection: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onToggleDirection,
        modifier =
            modifier
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
                AnimatedContent(
                    targetState = ascending,
                    label = "FilterDirection",
                ) { state ->
                    val iconId = if (state) {
                        UiCommonR.drawable.sort_ascending
                    } else {
                        UiCommonR.drawable.sort_descending
                    }

                    AdaptiveIcon(
                        painter = painterResource(iconId),
                        contentDescription = stringResource(LocaleR.string.sort_icon_content_desc),
                        tint = MaterialTheme.colorScheme.onSurface,
                        dp = 14.dp,
                    )
                }
            }

            Text(
                text = filter.displayName.asString(),
                style = MaterialTheme.typography.labelMedium.asAdaptiveTextStyle(),
                color = MaterialTheme.colorScheme.onSurface.copy(0.8f),
            )
        }
    }
}

@Preview
@Composable
private fun LibraryFilterPillPreview() {
    FlixclusiveTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            LibraryFilterPill(
                isSelected = false,
                filter = LibrarySortFilter.Name,
                ascending = true,
                onToggleDirection = {},
            )
        }
    }
}

@Preview
@Composable
private fun LibraryFilterPillDescendingPreview() {
    FlixclusiveTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            LibraryFilterPill(
                isSelected = true,
                filter = LibrarySortFilter.ModifiedAt,
                ascending = true,
                onToggleDirection = {},
            )
        }
    }
}
