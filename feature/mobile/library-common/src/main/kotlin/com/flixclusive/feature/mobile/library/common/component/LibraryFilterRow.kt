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
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.adaptive.AdaptiveIcon
import com.flixclusive.core.ui.common.util.adaptive.AdaptiveUiUtil.getAdaptiveDp
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.feature.mobile.library.common.util.LibraryFilterDirection
import com.flixclusive.feature.mobile.library.common.util.LibrarySortFilter
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import com.flixclusive.core.locale.R as LocaleR
import com.flixclusive.core.ui.common.R as UiCommonR

@Composable
fun LibraryFilterRow(
    isListEditable: Boolean,
    filters: ImmutableList<LibrarySortFilter>,
    currentFilter: LibrarySortFilter,
    currentDirection: LibraryFilterDirection,
    onUpdateFilter: (LibrarySortFilter) -> Unit,
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
                    vertical = 3.dp
                ),
                shape = MaterialTheme.shapes.small,
            ) {
                AdaptiveIcon(
                    painter = painterResource(UiCommonR.drawable.edit),
                    contentDescription = stringResource(LocaleR.string.multi_select),
                    tint = MaterialTheme.colorScheme.onSurface.onMediumEmphasis(),
                    dp = 16.dp,
                )
            }
        }

        item {
            VerticalDivider(
                modifier = Modifier
                    .height(buttonHeight)
                    .padding(5.dp),
                color = MaterialTheme.colorScheme.onSurface.onMediumEmphasis(),
            )
        }

        items(
            items = filters,
            key = { it.displayName.hashCode() },
        ) { filter ->
            LibraryFilterPill(
                isSelected = currentFilter == filter,
                filter = filter,
                direction = currentDirection,
                onToggleDirection = { onUpdateFilter(filter) },
            )
        }
    }
}

@Preview
@Composable
private fun LibraryFilterRowPreview() {
    var currentFilter by remember { mutableStateOf<LibrarySortFilter>(LibrarySortFilter.Name) }
    var currentDirection by remember { mutableStateOf(LibraryFilterDirection.ASC) }

    FlixclusiveTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            LibraryFilterRow(
                isListEditable = true,
                filters = persistentListOf(
                    LibrarySortFilter.Name,
                    LibrarySortFilter.AddedAt,
                    LibrarySortFilter.ModifiedAt,
                ),
                currentFilter = currentFilter,
                currentDirection = currentDirection,
                onUpdateFilter = { filter ->
                    if (currentFilter == filter) {
                        currentDirection = currentDirection.toggle()
                    } else {
                        currentFilter = filter
                        currentDirection = LibraryFilterDirection.ASC
                    }
                },
                onStartSelecting = {  },
            )
        }
    }
}
