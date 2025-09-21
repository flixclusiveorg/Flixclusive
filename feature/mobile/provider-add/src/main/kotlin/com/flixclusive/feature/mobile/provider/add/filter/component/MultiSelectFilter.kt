package com.flixclusive.feature.mobile.provider.add.filter.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.presentation.mobile.AdaptiveTextStyle.asAdaptiveTextStyle
import com.flixclusive.core.presentation.mobile.components.AdaptiveIcon
import com.flixclusive.core.presentation.mobile.util.AdaptiveSizeUtil.getAdaptiveDp
import com.flixclusive.feature.mobile.provider.add.filter.AddProviderFilterType
import com.flixclusive.feature.mobile.provider.add.filter.util.FilterChildPadding
import com.flixclusive.feature.mobile.provider.add.filter.util.toOptionString
import com.flixclusive.core.drawables.R as UiCommonR
import com.flixclusive.core.strings.R as LocaleR

@Composable
internal fun MultiSelectFilter(
    filter: AddProviderFilterType.MultiSelect,
    onUpdateFilter: (Set<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    BaseFilterGroup(
        modifier = modifier,
        title = filter.title.asString(),
    ) {
        val options = remember {
            filter.options.sortedBy {
                val index = filter.selectedValue.indexOf(it)
                if (index == -1) filter.selectedValue.size else index
            }
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(FilterChildPadding),
            contentPadding = PaddingValues(horizontal = FilterChildPadding),
            modifier = Modifier.fillMaxWidth(),
        ) {
            items(options, key = { it.toOptionString(context) }) {
                SelectableButton(
                    modifier = Modifier.animateItem(),
                    selected = filter.selectedValue.contains(it),
                    name = it,
                    onSelect = {
                        val newSet = filter.selectedValue.toMutableSet()
                        if (filter.selectedValue.contains(it)) {
                            newSet.remove(it)
                        } else {
                            newSet.add(it)
                        }

                        onUpdateFilter(newSet.toSet())
                    },
                )
            }
        }
    }
}

@Composable
private fun SelectableButton(
    selected: Boolean,
    name: String,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borders =
        if (selected) {
            BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary,
            )
        } else {
            ButtonDefaults.outlinedButtonBorder()
        }

    OutlinedButton(
        onClick = onSelect,
        contentPadding = PaddingValues(horizontal = 12.dp),
        shape = MaterialTheme.shapes.small,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface.copy(0.8F),
        ),
        border = borders,
        modifier = modifier
            .height(getAdaptiveDp(35.dp))
            .widthIn(min = getAdaptiveDp(80.dp))
            .graphicsLayer {
                alpha = if (selected) 1f else 0.6f
            },
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AnimatedVisibility(
                visible = selected,
                label = "CrossButton",
            ) {
                AdaptiveIcon(
                    painter = painterResource(UiCommonR.drawable.round_close_24),
                    contentDescription = stringResource(LocaleR.string.remove),
                    dp = 16.dp,
                )
            }

            Text(
                text = name,
                style = MaterialTheme.typography.labelMedium.asAdaptiveTextStyle(size = 15.sp),
                color = LocalContentColor.current.copy(0.8f),
            )
        }
    }
}
