package com.flixclusive.feature.mobile.user.pin.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.flixclusive.core.presentation.mobile.AdaptiveTextStyle.asAdaptiveTextStyle
import com.flixclusive.core.presentation.mobile.util.AdaptiveSizeUtil.getAdaptiveDp

@Composable
internal fun PinButton(
    digit: Int,
    onClick: () -> Unit,
) {
    PinButton(
        onClick = onClick,
    ) {
        Text(
            text = "$digit",
            fontWeight = FontWeight.Black,
            style = LocalTextStyle.current.asAdaptiveTextStyle(),
        )
    }
}

@Composable
internal fun PinButton(
    onClick: () -> Unit,
    enabled: Boolean = true,
    noEmphasis: Boolean = false,
    content: @Composable RowScope.() -> Unit,
) {
    if (noEmphasis) {
        TextButton(
            enabled = enabled,
            content = content,
            onClick = onClick,
            shape = MaterialTheme.shapes.small,
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier.size(getAdaptiveDp(65.dp)),
        )
    } else {
        OutlinedButton(
            enabled = enabled,
            content = content,
            onClick = onClick,
            shape = MaterialTheme.shapes.small,
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier
                .size(getAdaptiveDp(65.dp)),
        )
    }
}
