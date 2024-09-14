package com.flixclusive.feature.mobile.searchExpanded.component.filter

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.provider.filter.BottomSheetComponent
import com.flixclusive.provider.filter.Filter
import com.flixclusive.provider.filter.FilterGroup
import com.flixclusive.feature.mobile.searchExpanded.component.filter.component.FilterCheckbox
import com.flixclusive.feature.mobile.searchExpanded.component.filter.component.FilterTriStateCheckbox
import com.flixclusive.feature.mobile.searchExpanded.component.filter.component.SelectDropdownMenu
import com.flixclusive.feature.mobile.searchExpanded.component.filter.component.SelectRadioMenu
import com.flixclusive.feature.mobile.searchExpanded.component.filter.component.SortItems
import com.flixclusive.feature.mobile.searchExpanded.util.FilterBottomSheetStyle
import com.flixclusive.feature.mobile.searchExpanded.util.FilterBottomSheetStyle.FilterItemLargeLabelSize
import com.flixclusive.data.tmdb.TmdbFilters.Companion.getDefaultTmdbFilters

@Composable
internal fun FilterItem(
    filter: Filter<*>,
    filterGroup: FilterGroup,
    onUpdateFilters: () -> Unit
) {
    when (filter) {
        BottomSheetComponent.HorizontalDivider -> {
            HorizontalDivider()
        }
        is BottomSheetComponent.HeaderLabel -> {
            Text(
                text = filter.name,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = FilterItemLargeLabelSize,
                    color = LocalContentColor.current.onMediumEmphasis(FilterBottomSheetStyle.STRONGEST_EMPHASIS),
                    fontWeight = FontWeight.Black
                )
            )
        }
        is BottomSheetComponent.Spacer -> {
            Spacer(modifier = Modifier.padding(filter.state.dp))
        }
        is Filter.CheckBox -> {
            FilterCheckbox(
                label = filter.name,
                isChecked = filter.state,
                onCheckedChange = {
                    filter.state = it
                    onUpdateFilters()
                },
            )
        }
        is Filter.TriState -> {
            FilterTriStateCheckbox(
                label = filter.name,
                state = filter.state,
                onToggle = {
                    filter.state = it
                    onUpdateFilters()
                }
            )
        }
        is Filter.Select<*> -> {
            if (filter.options.size < 5 && filterGroup.size == 1) {
                SelectRadioMenu(
                    options = filter.options,
                    selected = filter.state,
                    onSelect = { option ->
                        filter.state = option
                        onUpdateFilters()
                    }
                )
            } else {
                val dropdownLabel = when (filterGroup.size) {
                    1 -> null
                    else -> filter.name
                }

                SelectDropdownMenu(
                    label = dropdownLabel,
                    options = filter.options,
                    selected = filter.state,
                    onSelect = { option ->
                        filter.state = option
                        onUpdateFilters()
                    }
                )
            }
        }
        is Filter.Sort<*> -> {
            SortItems(
                options = filter.options,
                selected = filter.state,
                onToggle = { selection ->
                    filter.state = selection
                    onUpdateFilters()
                }
            )
        }
    }
}

@Preview
@Composable
private fun FilterItemPreview() {
    FlixclusiveTheme {
        Surface {
            val filters = remember { getDefaultTmdbFilters().first() }

            FilterItem(
                filter = filters.first(),
                filterGroup = filters,
            ) {}
        }
    }
}