package com.flixclusive.feature.mobile.searchExpanded.component.filter.component

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.flixclusive.core.presentation.theme.FlixclusiveTheme
import com.flixclusive.core.presentation.mobile.components.CustomCheckbox
import com.flixclusive.feature.mobile.searchExpanded.util.FilterBottomSheetStyle

@Composable
internal fun FilterCheckbox(
    modifier: Modifier = Modifier,
    label: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    BaseTextButton(
        modifier = modifier,
        label = label,
        isSelected = isChecked,
        onClick = { onCheckedChange(!isChecked) }
    ) {
        CustomCheckbox(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = FilterBottomSheetStyle.getCheckboxColors()
        )
    }
}

@Preview
@Composable
private fun FilterCheckboxPreview() {
    FlixclusiveTheme {
        Surface {
            FilterCheckbox(
                label = "Label",
                isChecked = true,
                onCheckedChange = {}
            )
        }
    }
}
