package com.flixclusive.feature.mobile.searchExpanded.component.filter.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.flixclusive.feature.mobile.searchExpanded.util.FilterBottomSheetStyle

@Composable
internal fun BaseTextButton(
    modifier: Modifier = Modifier,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    prefix: @Composable RowScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = FilterBottomSheetStyle.TextButtonMinHeight)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top
        ) {
            prefix()

            Text(
                text = label,
                style = FilterBottomSheetStyle.getLabelStyle(isSelected = isSelected),
                modifier = Modifier
                    .weight(1F)
                    .align(Alignment.CenterVertically)
            )
        }
    }
}