package com.flixclusive.feature.mobile.searchExpanded.component.filter.component

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.tooling.preview.Preview
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.mobile.component.CustomTriStateCheckbox
import com.flixclusive.provider.filter.Filter
import com.flixclusive.feature.mobile.searchExpanded.util.FilterBottomSheetStyle.getCheckboxColors

private fun getNextState(currentState: MutableIntState): Int {
    val newState = (currentState.intValue + 1) % 3
    currentState.intValue = newState

    return newState
}

@Composable
internal fun FilterTriStateCheckbox(
    modifier: Modifier = Modifier,
    label: String,
    state: Int,
    onToggle: (Int) -> Unit,
) {
    require(state > -1 && state < 3) {
        "Invalid state: $state. State ordinal must be between 0 and 2"
    }

    val toggledState = remember { mutableIntStateOf(state) }
    val isChecked = remember(state) {
        when (state) {
            Filter.TriState.STATE_UNSELECTED -> false
            else -> true
        }
    }

    BaseTextButton(
        modifier = modifier,
        label = label,
        isSelected = isChecked,
        onClick = { onToggle(getNextState(toggledState)) }
    ) {
        CustomTriStateCheckbox(
            state = ToggleableState.entries[state],
            onClick = { onToggle(getNextState(toggledState)) },
            colors = getCheckboxColors()
        )
    }
}

@Preview
@Composable
private fun FilterCheckboxPreview() {
    var state by remember { mutableIntStateOf(0) }

    FlixclusiveTheme {
        Surface {
            FilterTriStateCheckbox(
                label = "Label",
                state = state,
                onToggle = { state = it }
            )
        }
    }
}