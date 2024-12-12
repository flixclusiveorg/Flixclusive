package com.flixclusive.feature.mobile.user.util

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.flixclusive.core.ui.common.util.onMediumEmphasis

internal object StylesUtil {
    @Composable
    fun getNonEmphasizedLabel(fontSize: TextUnit = 14.sp): TextStyle {
        return MaterialTheme.typography.labelLarge.copy(
            fontWeight = FontWeight.Normal,
            color = LocalContentColor.current.onMediumEmphasis(),
            fontSize = fontSize
        )
    }
}