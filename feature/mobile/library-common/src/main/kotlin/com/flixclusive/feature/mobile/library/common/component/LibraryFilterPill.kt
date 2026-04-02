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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flixclusive.core.presentation.mobile.components.AdaptiveIcon
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.core.presentation.mobile.util.AdaptiveSizeUtil.getAdaptiveDp
import com.flixclusive.core.presentation.mobile.util.AdaptiveTextStyle.asAdaptiveTextStyle
import com.flixclusive.data.database.repository.LibrarySort
import com.flixclusive.core.drawables.R as UiCommonR
import com.flixclusive.core.strings.R as LocaleR

@Composable
fun LibraryFilterPill(
    selected: () -> LibrarySort,
    filter: LibrarySort,
    onToggleDirection: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val resources = LocalResources.current
    val displayName = remember {
        when (filter) {
            is LibrarySort.Name -> resources.getString(LocaleR.string.name)
            is LibrarySort.Modified -> resources.getString(LocaleR.string.modified_at)
            is LibrarySort.Added -> resources.getString(LocaleR.string.created_at)
        }
    }

    OutlinedButton(
        onClick = onToggleDirection,
        modifier =
            modifier
                .height(getAdaptiveDp(29.dp))
                .widthIn(min = getAdaptiveDp(55.dp))
                .graphicsLayer {
                    val isSelected = when (filter) {
                        is LibrarySort.Name -> selected() is LibrarySort.Name
                        is LibrarySort.Modified -> selected() is LibrarySort.Modified
                        is LibrarySort.Added -> selected() is LibrarySort.Added
                    }

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
            AnimatedVisibility(
                when (filter) {
                    is LibrarySort.Name -> selected() is LibrarySort.Name
                    is LibrarySort.Modified -> selected() is LibrarySort.Modified
                    is LibrarySort.Added -> selected() is LibrarySort.Added
                }
            ) {
                AnimatedContent(
                    targetState = selected().ascending,
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
                text = displayName,
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
                selected = { LibrarySort.Name(ascending = false) },
                filter = LibrarySort.Name(),
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
                selected = { LibrarySort.Modified(ascending = false) },
                filter = LibrarySort.Modified(),
                onToggleDirection = {},
            )
        }
    }
}
