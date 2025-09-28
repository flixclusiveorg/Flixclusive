package com.flixclusive.feature.mobile.searchExpanded.util

import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.presentation.mobile.AdaptiveTextStyle.asAdaptiveTextStyle

internal object FilterBottomSheetStyle {
    const val STRONGEST_EMPHASIS = 0.8F
    private const val MEDIUM_EMPHASIS = 0.7F

    val FilterItemLargeLabelSize = 18.sp
    private val FilterItemLabelSize = 15.sp

    val TextButtonMinHeight = 35.dp

    @Composable
    fun emphasizedPrimaryContainer()
        = MaterialTheme.colorScheme.primary.copy(0.05F)

    @Composable
    fun getLabelStyle(isSelected: Boolean): TextStyle {
        return MaterialTheme.typography.labelLarge.copy(
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface.copy(
                alpha = if (isSelected) 1F else MEDIUM_EMPHASIS,
            ),
        ).asAdaptiveTextStyle(FilterItemLabelSize)
    }

    @Composable
    fun getCheckboxColors() = CheckboxDefaults.colors().copy(
        checkedCheckmarkColor = MaterialTheme.colorScheme.primary,
        checkedBorderColor = MaterialTheme.colorScheme.primary.copy(STRONGEST_EMPHASIS),
        checkedBoxColor = emphasizedPrimaryContainer(),
        uncheckedBorderColor = MaterialTheme.colorScheme.onSurface.copy(MEDIUM_EMPHASIS)
    )
}
