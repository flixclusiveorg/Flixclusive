package com.flixclusive.feature.mobile.provider.add.filter.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.flixclusive.core.ui.common.adaptive.AdaptiveIcon
import com.flixclusive.feature.mobile.provider.add.filter.AddProviderFilterType
import com.flixclusive.feature.mobile.provider.add.filter.util.FilterChildPadding
import com.flixclusive.feature.mobile.provider.add.filter.util.toOptionString
import com.flixclusive.core.locale.R as LocaleR
import com.flixclusive.core.ui.common.R as UiCommonR

@Composable
internal fun SortFilter(
    filter: AddProviderFilterType.Sort<*>,
    onUpdateFilter: (AddProviderFilterType.Sort.SortSelection) -> Unit,
    modifier: Modifier = Modifier,
) {
    BaseFilterGroup(
        modifier = modifier,
        title = filter.title.asString()
    ) {
        Column {
            repeat(filter.options.size) { index ->
                val option = filter.options[index]

                BaseTextButton(
                    label = option.toOptionString(),
                    isSelected = filter.selectedValue.index == index,
                    onClick = {
                        var newState = filter.selectedValue.updateSelection(index = index)
                        onUpdateFilter(newState)
                    },
                ) {
                    AnimatedContent(
                        targetState = filter.selectedValue,
                        label = "SortDirection",
                        modifier =
                            Modifier
                                .padding(horizontal = FilterChildPadding)
                                .align(Alignment.CenterVertically),
                    ) { selected ->
                        if (selected.index == index) {
                            val iconId =
                                when (selected.ascending) {
                                    true -> UiCommonR.drawable.sort_ascending
                                    else -> UiCommonR.drawable.sort_descending
                                }

                            AdaptiveIcon(
                                painter = painterResource(iconId),
                                contentDescription = stringResource(LocaleR.string.sort_icon_content_desc),
                                dp = 16.dp,
                                modifier = Modifier.align(Alignment.CenterVertically),
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun AddProviderFilterType.Sort.SortSelection.updateSelection(
    index: Int = this.index,
    ascending: Boolean = this.ascending,
): AddProviderFilterType.Sort.SortSelection {
    if (index == this.index) {
        return copy(ascending = !ascending)
    }

    return copy(
        index = index,
        ascending = true,
    )
}
