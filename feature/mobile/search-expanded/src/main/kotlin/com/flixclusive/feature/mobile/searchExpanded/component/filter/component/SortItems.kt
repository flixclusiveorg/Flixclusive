package com.flixclusive.feature.mobile.searchExpanded.component.filter.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.feature.mobile.searchExpanded.component.filter.util.toOptionString
import com.flixclusive.provider.filter.Filter
import com.flixclusive.core.locale.R as LocaleR
import com.flixclusive.core.ui.common.R as UiCommonR

/**
 *
 * Updates/toggles the selection of a [Filter.Sort.Selection]
 * */
private fun Filter.Sort.Selection.updateSelection(
    index: Int = this.index, ascending: Boolean = this.ascending
): Filter.Sort.Selection {
    if (index == this.index)
        return copy(ascending = !ascending)

    return copy(
        index = index,
        ascending = false
    )
}

@Composable
internal fun <T> SortItems(
    modifier: Modifier = Modifier,
    options: List<T>,
    selected: Filter.Sort.Selection?,
    onToggle: (Filter.Sort.Selection) -> Unit,
) {
    val context = LocalContext.current

    Column {
        repeat(options.size) { index ->
            BaseTextButton(
                modifier = modifier,
                label = options[index].toOptionString(),
                isSelected = selected?.index == index,
                onClick = {
                    var newState = selected
                        ?.updateSelection(index = index)

                    if (newState == null) {
                        newState = Filter.Sort.Selection(
                            index = index,
                            ascending = false
                        )
                    }

                    onToggle(newState)
                }
            ) {
                AnimatedContent(
                    targetState = selected,
                    label = "",
                    modifier = Modifier.size(16.dp)
                        .align(Alignment.CenterVertically)
                ) {
                    if (it != null && it.index == index) {
                        val iconId = when (it.ascending) {
                            true -> UiCommonR.drawable.sort_ascending
                            else -> UiCommonR.drawable.sort_descending
                        }

                        Icon(
                            painter = painterResource(id = iconId),
                            contentDescription = stringResource(id = LocaleR.string.sort_icon_content_desc),
                            modifier = Modifier
                                .size(16.dp)
                                .align(Alignment.CenterVertically)
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun FilterCheckboxPreview() {
    val selection = remember { mutableStateOf(Filter.Sort.Selection(0, false)) }

    FlixclusiveTheme {
        Surface {
            SortItems(
                options = listOf("Option 1", "Option 2", "Option 3", "Option 4", "Option 5"),
                selected = selection.value,
                onToggle = {
                    selection.value = it
                }
            )
        }
    }
}