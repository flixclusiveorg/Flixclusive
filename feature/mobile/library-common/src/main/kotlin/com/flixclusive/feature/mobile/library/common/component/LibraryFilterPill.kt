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
import androidx.compose.ui.unit.sp
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.adaptive.AdaptiveIcon
import com.flixclusive.core.ui.common.util.adaptive.AdaptiveStylesUtil.getAdaptiveTextStyle
import com.flixclusive.core.ui.common.util.adaptive.AdaptiveUiUtil.getAdaptiveDp
import com.flixclusive.core.ui.common.util.adaptive.TextStyleMode
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.feature.mobile.library.common.util.LibraryFilterDirection
import com.flixclusive.feature.mobile.library.common.util.LibrarySortFilter
import com.flixclusive.core.locale.R as LocaleR
import com.flixclusive.core.ui.common.R as UiCommonR

@Composable
fun LibraryFilterPill(
    isSelected: Boolean,
    filter: LibrarySortFilter,
    direction: LibraryFilterDirection,
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
                    targetState = direction,
                    label = "FilterDirection",
                ) { direction ->
                    val iconId = if (direction.isAscending) {
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
                style = getAdaptiveTextStyle(
                    mode = TextStyleMode.SemiEmphasized,
                    size = 12.sp,
                ),
                color = MaterialTheme.colorScheme.onSurface.onMediumEmphasis(0.8f),
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
                direction = LibraryFilterDirection.ASC,
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
                direction = LibraryFilterDirection.DESC,
                onToggleDirection = {},
            )
        }
    }
}
