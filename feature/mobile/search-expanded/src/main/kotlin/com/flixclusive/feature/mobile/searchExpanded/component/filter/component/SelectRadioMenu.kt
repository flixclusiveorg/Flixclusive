package com.flixclusive.feature.mobile.searchExpanded.component.filter.component

import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import com.flixclusive.core.presentation.mobile.util.AdaptiveTextStyle.asAdaptiveTextStyle
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.core.presentation.mobile.util.AdaptiveSizeUtil.getAdaptiveDp
import com.flixclusive.feature.mobile.searchExpanded.component.filter.util.toOptionString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SelectRadioMenu(
    options: List<*>,
    selected: Int?,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    SingleChoiceSegmentedButtonColumn(modifier = modifier) {
        options.fastForEachIndexed { index, option ->
            VerticalSegmentedButton(
                selected = index == selected,
                enabled = index != selected,
                onClick = { onSelect(index) },
                shape = getVerticalSegmentedShape(
                    index = index,
                    count = options.size
                ),
                modifier = Modifier
                    .heightIn(min = getAdaptiveDp(50.dp)),
            ) {
                Text(
                    text = option.toOptionString(),
                    style = LocalTextStyle.current.asAdaptiveTextStyle(),
                )
            }
        }
    }
}

@Preview
@Composable
private fun SelectRadioMenuPreview() {
    FlixclusiveTheme {
        Surface {
            SelectRadioMenu(
                options = listOf("Option 1", "Option 2"),
                selected = 0,
                onSelect = {}
            )
        }
    }
}
