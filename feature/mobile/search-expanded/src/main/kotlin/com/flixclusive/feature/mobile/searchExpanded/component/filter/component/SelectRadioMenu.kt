package com.flixclusive.feature.mobile.searchExpanded.component.filter.component

import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.mobile.component.SingleChoiceSegmentedButtonColumn
import com.flixclusive.core.ui.mobile.component.VerticalSegmentedButton
import com.flixclusive.core.ui.mobile.component.getVerticalSegmentedShape
import com.flixclusive.feature.mobile.searchExpanded.component.filter.util.toOptionString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SelectRadioMenu(
    modifier: Modifier = Modifier,
    options: List<*>,
    selected: Int?,
    onSelect: (Int) -> Unit,
) {
    SingleChoiceSegmentedButtonColumn(
        modifier = modifier
    ) {
        options.forEachIndexed { index, option ->
            VerticalSegmentedButton(
                selected = index == selected,
                enabled = index != selected,
                onClick = { onSelect(index) },
                shape = getVerticalSegmentedShape(
                    index = index,
                    count = options.size
                ),
                modifier = Modifier
                    .heightIn(min = 50.dp)
            ) {
                Text(
                    text = option.toOptionString()
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