package com.flixclusive.core.ui.tv

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.unit.Dp
import com.flixclusive.core.ui.common.util.ifElse

@Composable
fun NonFocusableSpacer(
    width: Dp = Dp.Unspecified,
    height: Dp = Dp.Unspecified
) {
    Spacer(
        modifier = Modifier
            .focusProperties {
                canFocus = false
            }
            .ifElse(
                width != Dp.Unspecified,
                Modifier.width(width)
            )
            .ifElse(
                height != Dp.Unspecified,
                Modifier.height(height)
            )
    )
}