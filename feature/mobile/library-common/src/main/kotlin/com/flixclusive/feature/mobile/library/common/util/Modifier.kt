package com.flixclusive.feature.mobile.library.common.util

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp

@Composable
fun Modifier.selectionBorder(
    isSelected: Boolean,
    selectedColor: Color = MaterialTheme.colorScheme.tertiary,
    unselectedColor: Color = Color.Transparent,
    width: Dp = Dp.Hairline,
    shape: Shape = RectangleShape,
) = border(
        shape = shape,
        border =
            BorderStroke(
                width = width,
                color =
                    if (isSelected) {
                        selectedColor
                    } else {
                        unselectedColor
                    },
    ),
)
