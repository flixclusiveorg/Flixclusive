package com.flixclusive.feature.mobile.library.common.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flixclusive.core.presentation.mobile.components.AdaptiveIcon
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.core.presentation.mobile.util.AdaptiveSizeUtil.getAdaptiveDp
import com.flixclusive.feature.mobile.library.common.util.LibrarySortFilter
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import com.flixclusive.core.drawables.R as UiCommonR
import com.flixclusive.core.strings.R as LocaleR

@Composable
fun LibraryFilterRow(
    isListEditable: Boolean,
    filters: ImmutableList<LibrarySortFilter>,
    selected: LibrarySortFilter,
    ascending: Boolean,
    onUpdate: (LibrarySortFilter) -> Unit,
    onStartSelecting: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val buttonHeight = getAdaptiveDp(29.dp)

    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
    ) {
        item(key = "select_button") {
            OutlinedButton(
                onClick = onStartSelecting,
                enabled = isListEditable,
                modifier = Modifier
                    .height(buttonHeight)
                    .width(getAdaptiveDp(32.dp)),
                contentPadding = PaddingValues(
                    horizontal = 4.dp,
                    vertical = 3.dp,
                ),
                shape = MaterialTheme.shapes.small,
            ) {
                AdaptiveIcon(
                    painter = painterResource(UiCommonR.drawable.edit),
                    contentDescription = stringResource(LocaleR.string.multi_select),
                    tint = MaterialTheme.colorScheme.onSurface.copy(0.6f),
                    dp = 16.dp,
                )
            }
        }

        item {
            VerticalDivider(
                modifier = Modifier
                    .height(buttonHeight)
                    .padding(5.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(0.6f),
            )
        }

        items(
            items = filters,
            key = { it.displayName.hashCode() },
        ) { filter ->
            LibraryFilterPill(
                isSelected = selected == filter,
                filter = filter,
                ascending = ascending,
                onToggleDirection = { onUpdate(filter) },
            )
        }
    }
}

@Preview
@Composable
private fun LibraryFilterRowPreview() {
    var currentFilter by remember { mutableStateOf<LibrarySortFilter>(LibrarySortFilter.Name) }
    var ascending by remember { mutableStateOf(true) }

    FlixclusiveTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            LibraryFilterRow(
                isListEditable = true,
                filters = persistentListOf(
                    LibrarySortFilter.Name,
                    LibrarySortFilter.AddedAt,
                    LibrarySortFilter.ModifiedAt,
                ),
                selected = currentFilter,
                ascending = ascending,
                onUpdate = { filter ->
                    if (currentFilter == filter) {
                        ascending = !ascending
                    } else {
                        currentFilter = filter
                        ascending = true
                    }
                },
                onStartSelecting = { },
            )
        }
    }
}
